package com.retrohall.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.io.InputStream

object FileUtils {
    private val forbidden = Regex("""[\\/:*?"<>|\u0000-\u001f]""")

    /** Makes a string safe to use as a file name. */
    fun sanitize(name: String): String = name.replace(forbidden, "_").trim().take(150).ifEmpty { "file" }

    fun displayName(resolver: ContentResolver, uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.lastPathSegment
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    /**
     * Copies [input] to [destination] through a temporary file so a half-written file never
     * looks complete. [onProgress] receives the number of bytes copied so far.
     */
    fun copyAtomically(input: InputStream, destination: File, onProgress: ((Long) -> Unit)? = null): Long {
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile, destination.name + ".part")
        var copied = 0L
        try {
            input.use { source ->
                temp.outputStream().use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        copied += read
                        onProgress?.invoke(copied)
                    }
                }
            }
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) throw IOException("无法写入 ${destination.name}")
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return copied
    }

    fun writeAtomically(destination: File, data: ByteArray) {
        copyAtomically(data.inputStream(), destination)
    }

    /** Reads up to [count] bytes, fewer only at end of stream. */
    fun readHeader(input: InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        var total = 0
        input.use {
            while (total < count) {
                val read = it.read(buffer, total, count - total)
                if (read < 0) break
                total += read
            }
        }
        return if (total == count) buffer else buffer.copyOf(total)
    }

    fun directorySize(directory: File): Long =
        directory.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** True when the file starts with the ELF magic number, i.e. it is a native library. */
    fun isElf(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val magic = ByteArray(4)
            input.read(magic) == 4 &&
                magic[0] == 0x7F.toByte() && magic[1] == 'E'.code.toByte() &&
                magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
        }
    }.getOrDefault(false)
}
