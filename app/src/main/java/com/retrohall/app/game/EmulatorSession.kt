package com.retrohall.app.game

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.PixelCopy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.retrohall.app.AppGraph
import com.retrohall.app.data.Cheat
import com.retrohall.app.data.Game
import com.retrohall.app.data.SaveManager
import com.retrohall.core.RetroKey
import com.retrohall.core.ShaderOption
import com.retrohall.core.StickMode
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Everything that talks to the running core: input, save states, battery saves, cheats, speed
 * and pausing. Calls that wait for the emulation thread (serialization, cheats) must not run on
 * the main thread unless noted.
 */
class EmulatorSession(val game: Game, val view: GLRetroView) : DefaultLifecycleObserver {
    private val saves = AppGraph.saves
    private val database = AppGraph.database
    private val mainHandler = Handler(Looper.getMainLooper())

    /** True once the first frame has been rendered, i.e. the game is loaded. */
    @Volatile
    var started = false
        private set

    private var menuPaused = false
    private var playingSince: Long? = null
    private var appliedCheats = emptyList<String>()

    fun sendKey(key: RetroKey, pressed: Boolean, port: Int = 0) {
        view.sendKeyEvent(if (pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, InputMapping.keyCode(key), port)
    }

    fun sendRawKey(action: Int, keyCode: Int, port: Int) {
        view.sendKeyEvent(action, keyCode, port)
    }

    fun sendStick(mode: StickMode, x: Float, y: Float, port: Int = 0) {
        view.sendMotionEvent(InputMapping.motionSource(mode), x, y, port)
    }

    /** Called after the first frame. Applies cheats and optionally resumes from the automatic save. */
    suspend fun onFirstFrame(cheats: List<Cheat>, loadAutoSave: Boolean): Boolean {
        started = true
        startCounting()
        applyCheats(cheats)
        return loadAutoSave && loadState(SaveManager.AUTO_SLOT)
    }

    suspend fun saveState(slot: Int, screenshot: Bitmap?): Boolean = withContext(Dispatchers.IO) {
        if (!started) return@withContext false
        val data = runCatching { view.serializeState() }.getOrNull()
        if (data == null || data.isEmpty()) return@withContext false
        runCatching { saves.writeState(game, slot, data, screenshot) }.isSuccess
    }

    suspend fun loadState(slot: Int): Boolean = withContext(Dispatchers.IO) {
        if (!started) return@withContext false
        val data = saves.readState(game, slot) ?: return@withContext false
        runCatching { view.unserializeState(data) }.getOrDefault(false)
    }

    /**
     * Enables or disables cheats by position. Cheats removed since the last call are switched off
     * so the core does not keep applying them.
     */
    suspend fun applyCheats(cheats: List<Cheat>) = withContext(Dispatchers.IO) {
        if (!started) return@withContext
        cheats.forEachIndexed { index, cheat ->
            runCatching { view.setCheat(index, cheat.enabled, cheat.code) }
        }
        for (index in cheats.size until appliedCheats.size) {
            runCatching { view.setCheat(index, false, appliedCheats[index]) }
        }
        appliedCheats = cheats.map { it.code }
    }

    fun setSpeed(multiplier: Int) {
        view.frameSpeed = multiplier
    }

    fun setShader(option: ShaderOption) {
        view.shader = InputMapping.shaderConfig(option)
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        if (started) view.reset()
    }

    /** Number of discs and the current one, for multi-disc games. */
    suspend fun disks(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (!started) return@withContext 0 to 0
        runCatching { view.getAvailableDisks() to view.getCurrentDisk() }.getOrDefault(0 to 0)
    }

    suspend fun changeDisk(index: Int) = withContext(Dispatchers.IO) {
        runCatching { view.changeDisk(index) }
    }

    /** Captures the current frame of the game view (without the touch controls drawn on top). */
    suspend fun screenshot(): Bitmap? = suspendCancellableCoroutine { continuation ->
        if (!started || view.width <= 0 || view.height <= 0 || !view.holder.surface.isValid) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(view, bitmap, { result ->
                if (continuation.isActive) continuation.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
            }, mainHandler)
        } catch (e: IllegalArgumentException) {
            continuation.resume(null)
        }
    }

    /** Freezes emulation while the in-game menu is open. Main thread. */
    fun pauseForMenu() {
        if (menuPaused) return
        menuPaused = true
        flushPlayTime()
        view.audioEnabled = false
        view.onPause()
    }

    fun resumeFromMenu() {
        if (!menuPaused) return
        menuPaused = false
        view.onResume()
        view.audioEnabled = true
        startCounting()
    }

    /** Writes the battery save and play time. Main thread; the GL thread still runs queued work while paused. */
    fun persist() {
        flushPlayTime()
        if (!started) return
        val sram = runCatching { view.serializeSRAM() }.getOrNull() ?: return
        runCatching { saves.writeSram(game, sram) }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!menuPaused) {
            startCounting()
            return
        }
        // LibretroDroid resumes its view right after this callback; keep it frozen while the menu is open.
        mainHandler.post {
            if (menuPaused) {
                view.audioEnabled = false
                view.onPause()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        persist()
    }

    private fun startCounting() {
        if (started && playingSince == null) playingSince = SystemClock.elapsedRealtime()
    }

    /**
     * Records play time. The write is a single small UPDATE done synchronously, because the game
     * process is killed right after the activity finishes and a queued write could be lost.
     */
    private fun flushPlayTime() {
        val since = playingSince ?: return
        playingSince = null
        val elapsed = SystemClock.elapsedRealtime() - since
        runCatching { database.addPlayTime(game.id, elapsed) }
    }
}
