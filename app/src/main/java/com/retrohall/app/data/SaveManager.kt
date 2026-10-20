package com.retrohall.app.data

import android.content.Context
import android.graphics.Bitmap
import com.retrohall.core.GameTitles
import com.retrohall.core.Platform
import java.io.File

data class SaveSlot(
    val index: Int,
    val exists: Boolean,
    val savedAt: Long,
    val screenshot: File?,
) {
    val isAuto: Boolean get() = index == SaveManager.AUTO_SLOT
}

/**
 * Battery saves (SRAM) and save states. Files are keyed by platform and ROM file name rather than
 * database id, so saves survive removing and re-adding a game.
 */
class SaveManager(context: Context) {
    private val statesRoot = File(context.filesDir, "states")
    private val savesRoot = File(context.filesDir, "saves")

    /** Directory given to the core for its own save files (memory cards and the like). */
    fun coreSavesDir(platform: Platform): File = File(savesRoot, platform.name).apply { mkdirs() }

    fun readSram(game: Game): ByteArray? = sramFile(game).takeIf { it.isFile }?.readBytes()

    fun writeSram(game: Game, data: ByteArray) {
        if (data.isEmpty()) return
        FileUtils.writeAtomically(sramFile(game), data)
    }

    fun slots(game: Game): List<SaveSlot> = (AUTO_SLOT..SLOT_COUNT).map { index ->
        val state = stateFile(game, index)
        val screenshot = screenshotFile(game, index)
        SaveSlot(
            index = index,
            exists = state.isFile,
            savedAt = if (state.isFile) state.lastModified() else 0,
            screenshot = screenshot.takeIf { state.isFile && it.isFile },
        )
    }

    fun hasState(game: Game, slot: Int): Boolean = stateFile(game, slot).isFile

    fun readState(game: Game, slot: Int): ByteArray? = stateFile(game, slot).takeIf { it.isFile }?.readBytes()

    fun writeState(game: Game, slot: Int, data: ByteArray, screenshot: Bitmap?) {
        FileUtils.writeAtomically(stateFile(game, slot), data)
        val image = screenshotFile(game, slot)
        if (screenshot == null) {
            image.delete()
            return
        }
        val scaled = scaleForThumbnail(screenshot)
        image.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (scaled !== screenshot) scaled.recycle()
    }

    private fun scaleForThumbnail(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= THUMBNAIL_WIDTH) return bitmap
        val height = (bitmap.height * THUMBNAIL_WIDTH.toFloat() / bitmap.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, height, true)
    }

    private fun gameKey(game: Game): String = FileUtils.sanitize(GameTitles.baseName(game.fileName))

    private fun sramFile(game: Game) = File(File(savesRoot, game.platform.name), gameKey(game) + ".srm")

    private fun stateDir(game: Game) = File(File(statesRoot, game.platform.name), gameKey(game))

    private fun stateFile(game: Game, slot: Int) = File(stateDir(game), "slot$slot.state")

    private fun screenshotFile(game: Game, slot: Int) = File(stateDir(game), "slot$slot.png")

    companion object {
        /** Slot written automatically when leaving a game and read when it starts again. */
        const val AUTO_SLOT = 0

        /** Number of manual slots, numbered from 1. */
        const val SLOT_COUNT = 6

        private const val THUMBNAIL_WIDTH = 400
    }
}
