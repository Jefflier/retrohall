package com.retrohall.app.game

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retrohall.app.data.Cheat
import com.retrohall.app.data.Game
import com.retrohall.app.data.SaveSlot
import com.retrohall.core.RetroKey
import com.retrohall.core.ShaderOption
import com.retrohall.core.StickMode
import com.swordfish.libretrodroid.GLRetroView

sealed interface GamePhase {
    data class Loading(val message: String) : GamePhase

    data object Running : GamePhase

    data class Failed(val message: String) : GamePhase
}

enum class MenuPage(val title: String) {
    MAIN("游戏菜单"),
    SAVE("保存进度"),
    LOAD("读取进度"),
    CHEATS("金手指"),
}

/** Observable state of the game screen, owned by [GameActivity]. */
class GameUiState {
    var game by mutableStateOf<Game?>(null)
    var phase by mutableStateOf<GamePhase>(GamePhase.Loading("正在准备游戏…"))
    var retroView by mutableStateOf<GLRetroView?>(null)
    var menuOpen by mutableStateOf(false)
    var menuPage by mutableStateOf(MenuPage.MAIN)
    var fastForward by mutableStateOf(false)
    var showPad by mutableStateOf(true)
    var shader by mutableStateOf(ShaderOption.DEFAULT)
    var slots by mutableStateOf<List<SaveSlot>>(emptyList())
    var cheats by mutableStateOf<List<Cheat>>(emptyList())

    /** Disc count and current disc index of multi-disc games. */
    var disks by mutableStateOf(0 to 0)
}

/** Actions the game screen can trigger. */
interface GameController {
    fun onVirtualKey(key: RetroKey, pressed: Boolean)
    fun onVirtualStick(mode: StickMode, x: Float, y: Float)
    fun onBack()
    fun openMenu()
    fun closeMenu()
    fun toggleFastForward()
    fun saveToSlot(slot: Int)
    fun loadFromSlot(slot: Int)
    fun cycleShader()
    fun togglePad()
    fun nextDisk()
    fun resetGame()
    fun exitGame()
    fun setCheatEnabled(cheat: Cheat, enabled: Boolean)
    fun addCheat(description: String, code: String)
    fun deleteCheat(cheat: Cheat)
    fun importCheats(uri: Uri)
}
