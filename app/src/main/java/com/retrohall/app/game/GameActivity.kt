package com.retrohall.app.game

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.retrohall.app.AppGraph
import com.retrohall.app.data.AppSettings
import com.retrohall.app.data.Cheat
import com.retrohall.app.data.Game
import com.retrohall.app.data.SaveManager
import com.retrohall.app.ui.theme.RetroHallTheme
import com.retrohall.core.CheatCode
import com.retrohall.core.CheatParser
import com.retrohall.core.OrientationOption
import com.retrohall.core.RetroKey
import com.retrohall.core.ShaderOption
import com.retrohall.core.StickMode
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/**
 * Runs one game. Lives in the `:game` process (see the manifest), which is killed when the game
 * ends so every launch gets a clean native state.
 */
class GameActivity : ComponentActivity(), GameController {
    private val ui = GameUiState()
    private lateinit var settings: AppSettings
    private var game: Game? = null
    private var session: EmulatorSession? = null

    /** Frame captured when the menu opened, used as the save-slot thumbnail. */
    private var menuScreenshot: Bitmap? = null
    private val triggerPressed = BooleanArray(2)
    private var exiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppGraph.settings.current
        ui.shader = settings.shader
        ui.showPad = !(settings.hidePadWithController && hasPhysicalGamepad())
        requestedOrientation = when (settings.orientation) {
            OrientationOption.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            OrientationOption.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationOption.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            RetroHallTheme(darkTheme = true) {
                GameScreen(state = ui, settings = settings, controller = this)
            }
        }
        // The insets controller is only reliable once the content view exists.
        enterImmersiveMode()
        val gameId = intent.getLongExtra(EXTRA_GAME_ID, -1L)
        lifecycleScope.launch { startGame(gameId) }
    }

    private suspend fun startGame(gameId: Long) {
        val game = AppGraph.games.game(gameId) ?: return fail("找不到这个游戏，可能已被移除")
        this.game = game
        ui.game = game

        val core = game.platform.core
        val coreFile = AppGraph.cores.coreFile(core)
        if (!coreFile.isFile) return fail("尚未安装 ${core.displayName} 核心，请先在「核心」页下载")

        val rom = try {
            AppGraph.roms.prepare(game) { message -> ui.phase = GamePhase.Loading(message) }
        } catch (e: Exception) {
            return fail("无法读取游戏文件：${e.message}")
        }
        ui.phase = GamePhase.Loading("正在启动 ${core.displayName}…")
        val sram = withContext(Dispatchers.IO) {
            AppGraph.database.recordLaunch(game.id)
            AppGraph.saves.readSram(game)
        }

        val data = GLRetroViewData(this).apply {
            coreFilePath = coreFile.absolutePath
            gameFilePath = rom.absolutePath
            systemDirectory = AppGraph.bios.systemDir.absolutePath
            savesDirectory = AppGraph.saves.coreSavesDir(game.platform).absolutePath
            variables = game.platform.coreVariables.map { (key, value) -> Variable(key = key, value = value) }.toTypedArray()
            saveRAMState = sram
            shader = InputMapping.shaderConfig(settings.shader)
            rumbleEventsEnabled = false
            preferLowLatencyAudio = true
        }
        val view = GLRetroView(this, data).apply {
            // Physical controllers are routed through dispatchKeyEvent instead of the view's own handler.
            isFocusable = false
            isFocusableInTouchMode = false
        }
        lifecycle.addObserver(view)
        val session = EmulatorSession(game, view)
        lifecycle.addObserver(session)
        this.session = session
        ui.retroView = view
        ui.phase = GamePhase.Running

        lifecycleScope.launch {
            view.getGLRetroErrors().collect { code -> fail(errorMessage(code)) }
        }
        lifecycleScope.launch {
            view.getGLRetroEvents().first { it is GLRetroView.GLRetroEvents.FrameRendered }
            val cheats = AppGraph.games.cheats(game.id)
            ui.cheats = cheats
            if (session.onFirstFrame(cheats, settings.autoLoadOnStart)) toast("已继续上次的进度")
        }
    }

    private fun fail(message: String) {
        ui.menuOpen = false
        ui.phase = GamePhase.Failed(message)
    }

    private fun errorMessage(code: Int): String = when (code) {
        GLRetroView.ERROR_LOAD_LIBRARY -> "核心加载失败，请在「核心」页重新下载该核心"
        GLRetroView.ERROR_LOAD_GAME -> "游戏加载失败：ROM 可能已损坏，或与该核心不兼容"
        GLRetroView.ERROR_GL_NOT_COMPATIBLE -> "此设备的 OpenGL ES 版本不受支持"
        GLRetroView.ERROR_SERIALIZATION -> "存档数据无法读取"
        GLRetroView.ERROR_CHEAT -> "金手指代码格式不正确"
        else -> "模拟器发生未知错误"
    }

    // region GameController

    override fun onVirtualKey(key: RetroKey, pressed: Boolean) {
        session?.sendKey(key, pressed)
    }

    override fun onVirtualStick(mode: StickMode, x: Float, y: Float) {
        session?.sendStick(mode, x, y)
    }

    override fun onBack() {
        when {
            ui.phase is GamePhase.Failed -> exitGame()
            !ui.menuOpen -> openMenu()
            ui.menuPage != MenuPage.MAIN -> ui.menuPage = MenuPage.MAIN
            else -> closeMenu()
        }
    }

    override fun openMenu() {
        val session = session ?: return
        if (ui.menuOpen || ui.phase !is GamePhase.Running) return
        ui.menuPage = MenuPage.MAIN
        ui.menuOpen = true
        lifecycleScope.launch {
            menuScreenshot = session.screenshot()
            if (!ui.menuOpen) return@launch
            session.pauseForMenu()
            refreshMenuData()
        }
    }

    override fun closeMenu() {
        if (!ui.menuOpen) return
        ui.menuOpen = false
        session?.resumeFromMenu()
    }

    private suspend fun refreshMenuData() {
        val game = game ?: return
        val session = session ?: return
        ui.slots = withContext(Dispatchers.IO) { AppGraph.saves.slots(game) }
        ui.cheats = AppGraph.games.cheats(game.id)
        ui.disks = session.disks()
    }

    override fun toggleFastForward() {
        ui.fastForward = !ui.fastForward
        session?.setSpeed(if (ui.fastForward) settings.fastForwardSpeed else 1)
    }

    override fun saveToSlot(slot: Int) {
        val session = session ?: return
        lifecycleScope.launch {
            val saved = session.saveState(slot, menuScreenshot)
            toast(if (saved) "已保存到存档 $slot" else "保存失败，此核心可能不支持即时存档")
            closeMenu()
        }
    }

    override fun loadFromSlot(slot: Int) {
        val session = session ?: return
        lifecycleScope.launch {
            val loaded = session.loadState(slot)
            toast(if (loaded) "已读取${if (slot == SaveManager.AUTO_SLOT) "自动存档" else "存档 $slot"}" else "读取失败")
            closeMenu()
        }
    }

    override fun cycleShader() {
        val options = ShaderOption.entries
        val next = options[(ui.shader.ordinal + 1) % options.size]
        ui.shader = next
        session?.setShader(next)
    }

    override fun togglePad() {
        ui.showPad = !ui.showPad
    }

    override fun nextDisk() {
        val session = session ?: return
        val (count, current) = ui.disks
        if (count <= 1) return
        val next = (current + 1) % count
        lifecycleScope.launch {
            session.changeDisk(next)
            ui.disks = count to next
            toast("已切换到第 ${next + 1} 张光盘")
        }
    }

    override fun resetGame() {
        val session = session ?: return
        lifecycleScope.launch {
            session.reset()
            closeMenu()
        }
    }

    override fun exitGame() {
        if (exiting) return
        exiting = true
        val session = session
        lifecycleScope.launch {
            if (session != null && session.started && settings.autoSaveOnExit) {
                ui.phase = GamePhase.Loading("正在保存进度…")
                val screenshot = menuScreenshot ?: session.screenshot()
                session.saveState(SaveManager.AUTO_SLOT, screenshot)
            }
            session?.persist()
            finish()
        }
    }

    override fun setCheatEnabled(cheat: Cheat, enabled: Boolean) {
        lifecycleScope.launch {
            AppGraph.games.setCheatEnabled(cheat, enabled)
            reloadCheats()
        }
    }

    override fun addCheat(description: String, code: String) {
        val game = game ?: return
        val normalized = CheatParser.normalizeCode(code)
        if (normalized.isEmpty()) return
        lifecycleScope.launch {
            val name = description.trim().ifEmpty { "金手指 ${ui.cheats.size + 1}" }
            AppGraph.games.addCheats(game.id, listOf(CheatCode(name, normalized, enabled = true)))
            reloadCheats()
        }
    }

    override fun deleteCheat(cheat: Cheat) {
        lifecycleScope.launch {
            AppGraph.games.deleteCheat(cheat)
            reloadCheats()
        }
    }

    override fun importCheats(uri: Uri) {
        val game = game ?: return
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            val cheats = text?.let(CheatParser::parse).orEmpty()
            if (cheats.isEmpty()) {
                toast("文件中没有可识别的金手指")
                return@launch
            }
            AppGraph.games.addCheats(game.id, cheats)
            reloadCheats()
            toast("已导入 ${cheats.size} 条金手指")
        }
    }

    private suspend fun reloadCheats() {
        val game = game ?: return
        val cheats = AppGraph.games.cheats(game.id)
        ui.cheats = cheats
        session?.applyCheats(cheats)
    }

    // endregion

    // region Physical controllers and keyboards

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.action == KeyEvent.ACTION_UP) onBack()
            return true
        }
        val session = session
        if (session != null && !ui.menuOpen && ui.phase is GamePhase.Running) {
            val mapped = InputMapping.mapPhysicalKey(event)
            if (mapped != null) {
                if (event.repeatCount == 0) session.sendRawKey(event.action, mapped, InputMapping.port(event.device))
                if (InputMapping.isGamepad(event)) onPhysicalInput()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val session = session
        if (session == null || ui.menuOpen ||
            !event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.actionMasked != MotionEvent.ACTION_MOVE
        ) {
            return super.dispatchGenericMotionEvent(event)
        }
        val port = InputMapping.port(event.device)
        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        // Systems without an analog stick also accept the left stick as a d-pad.
        val stickAsDpad = game?.platform?.layout?.stick == StickMode.DPAD
        val dpadX = if (abs(hatX) > 0.5f) sign(hatX) else if (stickAsDpad && abs(x) > 0.5f) sign(x) else 0f
        val dpadY = if (abs(hatY) > 0.5f) sign(hatY) else if (stickAsDpad && abs(y) > 0.5f) sign(y) else 0f
        session.sendStick(StickMode.DPAD, dpadX, dpadY, port)
        session.sendStick(StickMode.ANALOG_LEFT, x, y, port)
        session.sendStick(
            StickMode.ANALOG_RIGHT,
            event.getAxisValue(MotionEvent.AXIS_Z),
            event.getAxisValue(MotionEvent.AXIS_RZ),
            port,
        )
        updateTrigger(0, max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE)), RetroKey.L2, port)
        updateTrigger(1, max(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS)), RetroKey.R2, port)
        onPhysicalInput()
        return true
    }

    /** Analog triggers report axes; turn them into L2 / R2 presses. */
    private fun updateTrigger(index: Int, value: Float, key: RetroKey, port: Int) {
        val pressed = value > 0.5f
        if (pressed == triggerPressed[index]) return
        triggerPressed[index] = pressed
        session?.sendKey(key, pressed, port)
    }

    private fun onPhysicalInput() {
        if (ui.showPad && settings.hidePadWithController) ui.showPad = false
    }

    private fun hasPhysicalGamepad(): Boolean = InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        !device.isVirtual &&
            (device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    // endregion

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        // LibretroDroid has torn the core down by now (it observes ON_DESTROY). Ending the process
        // guarantees that the next game does not inherit global state from this core.
        if (isFinishing) Process.killProcess(Process.myPid())
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Deprecated on API 35, where edge-to-edge is always on; still needed on 30-34.
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_GAME_ID = "game_id"

        fun intent(context: Context, gameId: Long): Intent =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_GAME_ID, gameId)
    }
}
