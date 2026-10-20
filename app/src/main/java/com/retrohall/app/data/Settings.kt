package com.retrohall.app.data

import android.content.Context
import com.retrohall.core.CoreSource
import com.retrohall.core.OrientationOption
import com.retrohall.core.ShaderOption
import com.retrohall.core.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val shader: ShaderOption = ShaderOption.SHARP,
    val orientation: OrientationOption = OrientationOption.AUTO,
    val padOpacity: Float = 0.55f,
    val padScale: Float = 1f,
    val haptics: Boolean = true,
    val hidePadWithController: Boolean = true,
    val autoSaveOnExit: Boolean = true,
    val autoLoadOnStart: Boolean = true,
    val fastForwardSpeed: Int = 2,
    val coreSource: CoreSource = CoreSource.GITHUB,
    val githubMirror: String = "",
    val sortOrder: SortOrder = SortOrder.TITLE,
    /** Persisted SAF tree URIs of the library folders. */
    val romFolders: Set<String> = emptySet(),
)

/**
 * Settings stored in SharedPreferences. The game process reads them once at start; it runs in a
 * fresh process for every game, so it always sees what the main process last wrote.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    val current: AppSettings get() = _state.value

    @Synchronized
    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_state.value)
        write(updated)
        _state.value = updated
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            shader = enumOrDefault(prefs.getString("shader", null), defaults.shader),
            orientation = enumOrDefault(prefs.getString("orientation", null), defaults.orientation),
            padOpacity = prefs.getFloat("pad_opacity", defaults.padOpacity),
            padScale = prefs.getFloat("pad_scale", defaults.padScale),
            haptics = prefs.getBoolean("haptics", defaults.haptics),
            hidePadWithController = prefs.getBoolean("hide_pad_with_controller", defaults.hidePadWithController),
            autoSaveOnExit = prefs.getBoolean("auto_save_on_exit", defaults.autoSaveOnExit),
            autoLoadOnStart = prefs.getBoolean("auto_load_on_start", defaults.autoLoadOnStart),
            fastForwardSpeed = prefs.getInt("fast_forward_speed", defaults.fastForwardSpeed),
            coreSource = enumOrDefault(prefs.getString("core_source", null), defaults.coreSource),
            githubMirror = prefs.getString("github_mirror", null) ?: defaults.githubMirror,
            sortOrder = enumOrDefault(prefs.getString("sort_order", null), defaults.sortOrder),
            romFolders = prefs.getStringSet("rom_folders", null)?.toSet() ?: defaults.romFolders,
        )
    }

    private fun write(settings: AppSettings) {
        prefs.edit()
            .putString("shader", settings.shader.name)
            .putString("orientation", settings.orientation.name)
            .putFloat("pad_opacity", settings.padOpacity)
            .putFloat("pad_scale", settings.padScale)
            .putBoolean("haptics", settings.haptics)
            .putBoolean("hide_pad_with_controller", settings.hidePadWithController)
            .putBoolean("auto_save_on_exit", settings.autoSaveOnExit)
            .putBoolean("auto_load_on_start", settings.autoLoadOnStart)
            .putInt("fast_forward_speed", settings.fastForwardSpeed)
            .putString("core_source", settings.coreSource.name)
            .putString("github_mirror", settings.githubMirror)
            .putString("sort_order", settings.sortOrder.name)
            .putStringSet("rom_folders", settings.romFolders)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
