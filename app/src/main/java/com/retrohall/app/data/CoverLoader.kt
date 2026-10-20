package com.retrohall.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.retrohall.core.GameTitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Loads box art: a custom cover picked by the user, or the libretro-thumbnails image matching the
 * ROM name. Images are cached in memory and on disk; misses are remembered for a week so a
 * library of unmatched names does not hit the network on every visit.
 */
class CoverLoader(context: Context) {
    private val diskDir = File(context.cacheDir, "covers").apply { mkdirs() }
    private val network = Semaphore(4)
    private val memory = object : LruCache<String, Bitmap>(MEMORY_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun key(game: Game): String = game.coverPath ?: GameTitles.thumbnailUrl(game.platform, game.fileName)

    fun cached(game: Game): Bitmap? = memory.get(key(game))

    suspend fun load(game: Game): Bitmap? = withContext(Dispatchers.IO) {
        val key = key(game)
        memory.get(key)?.let { return@withContext it }
        val bitmap = if (game.coverPath != null) decode(File(game.coverPath)) else loadRemote(key)
        bitmap?.also { memory.put(key, it) }
    }

    /** Drops the cached image for a game, e.g. after the user picked a new cover. */
    fun invalidate(game: Game) {
        memory.remove(key(game))
    }

    private suspend fun loadRemote(url: String): Bitmap? {
        val hash = md5(url)
        val file = File(diskDir, "$hash.img")
        val miss = File(diskDir, "$hash.miss")
        if (file.isFile) return decode(file)
        if (miss.isFile && System.currentTimeMillis() - miss.lastModified() < MISS_TTL_MS) return null

        network.withPermit {
            val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", "RetroHall")
                when (connection.responseCode) {
                    in 200..299 -> FileUtils.copyAtomically(connection.inputStream, file)
                    404 -> {
                        miss.createNewFile()
                        return null
                    }
                    else -> return null
                }
            } catch (e: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return decode(file)
    }

    private fun decode(file: File): Bitmap? {
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_WIDTH) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val MEMORY_BYTES = 24 * 1024 * 1024
        private const val TARGET_WIDTH = 320
        private const val MISS_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
