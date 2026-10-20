package com.retrohall.app.game

import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import com.retrohall.core.RetroKey
import com.retrohall.core.ShaderOption
import com.retrohall.core.StickMode
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.ShaderConfig

/** Translates between the app's input model and the key codes / motion sources LibretroDroid expects. */
object InputMapping {
    /** LibretroDroid maps Android button key codes to RetroPad buttons of the same name. */
    fun keyCode(key: RetroKey): Int = when (key) {
        RetroKey.A -> KeyEvent.KEYCODE_BUTTON_A
        RetroKey.B -> KeyEvent.KEYCODE_BUTTON_B
        RetroKey.X -> KeyEvent.KEYCODE_BUTTON_X
        RetroKey.Y -> KeyEvent.KEYCODE_BUTTON_Y
        RetroKey.L1 -> KeyEvent.KEYCODE_BUTTON_L1
        RetroKey.L2 -> KeyEvent.KEYCODE_BUTTON_L2
        RetroKey.R1 -> KeyEvent.KEYCODE_BUTTON_R1
        RetroKey.R2 -> KeyEvent.KEYCODE_BUTTON_R2
        RetroKey.L3 -> KeyEvent.KEYCODE_BUTTON_THUMBL
        RetroKey.R3 -> KeyEvent.KEYCODE_BUTTON_THUMBR
        RetroKey.SELECT -> KeyEvent.KEYCODE_BUTTON_SELECT
        RetroKey.START -> KeyEvent.KEYCODE_BUTTON_START
    }

    fun motionSource(mode: StickMode): Int = when (mode) {
        StickMode.DPAD -> GLRetroView.MOTION_SOURCE_DPAD
        StickMode.ANALOG_LEFT -> GLRetroView.MOTION_SOURCE_ANALOG_LEFT
        StickMode.ANALOG_RIGHT -> GLRetroView.MOTION_SOURCE_ANALOG_RIGHT
    }

    fun shaderConfig(option: ShaderOption): ShaderConfig = when (option) {
        ShaderOption.DEFAULT -> ShaderConfig.Default
        ShaderOption.SHARP -> ShaderConfig.Sharp
        ShaderOption.CRT -> ShaderConfig.CRT
        ShaderOption.LCD -> ShaderConfig.LCD
        ShaderOption.SMOOTH -> ShaderConfig.CUT2()
    }

    private val dpadKeys = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    private val gamepadKeys = dpadKeys + setOf(
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_START,
    )

    fun isGamepad(event: InputEvent): Boolean =
        event.isFromSource(InputDevice.SOURCE_GAMEPAD) || event.isFromSource(InputDevice.SOURCE_JOYSTICK)

    /**
     * Maps a key from a controller or keyboard to a LibretroDroid key code, or null if the key is not
     * a game input.
     */
    fun mapPhysicalKey(event: KeyEvent): Int? {
        val code = event.keyCode
        if (isGamepad(event) || (event.isFromSource(InputDevice.SOURCE_DPAD) && code in dpadKeys)) {
            if (code !in gamepadKeys) return null
            // Android names face buttons by their position on an Xbox pad (A at the bottom), while the
            // RetroPad uses SNES positions (B at the bottom), so the pairs are swapped.
            return when (code) {
                KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
                KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
                KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
                KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
                else -> code
            }
        }
        if (event.isFromSource(InputDevice.SOURCE_KEYBOARD)) {
            return when (code) {
                in dpadKeys -> code
                KeyEvent.KEYCODE_Z -> KeyEvent.KEYCODE_BUTTON_B
                KeyEvent.KEYCODE_X -> KeyEvent.KEYCODE_BUTTON_A
                KeyEvent.KEYCODE_A -> KeyEvent.KEYCODE_BUTTON_Y
                KeyEvent.KEYCODE_S -> KeyEvent.KEYCODE_BUTTON_X
                KeyEvent.KEYCODE_Q -> KeyEvent.KEYCODE_BUTTON_L1
                KeyEvent.KEYCODE_W -> KeyEvent.KEYCODE_BUTTON_R1
                KeyEvent.KEYCODE_E -> KeyEvent.KEYCODE_BUTTON_L2
                KeyEvent.KEYCODE_R -> KeyEvent.KEYCODE_BUTTON_R2
                KeyEvent.KEYCODE_ENTER -> KeyEvent.KEYCODE_BUTTON_START
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.KEYCODE_BUTTON_SELECT
                else -> null
            }
        }
        return null
    }

    /** LibretroDroid supports four ports; controller numbers start at 1, keyboards report 0. */
    fun port(device: InputDevice?): Int = ((device?.controllerNumber ?: 0) - 1).coerceIn(0, 3)
}
