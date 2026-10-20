package com.retrohall.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.retrohall.core.Platform

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB300),
    onPrimary = Color(0xFF2A1F00),
    primaryContainer = Color(0xFF4D3A00),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF16394A),
    onSecondaryContainer = Color(0xFFC5E7FF),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2B2E36),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF181B21),
    surfaceContainer = Color(0xFF1D2027),
    surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF31343C),
    outline = Color(0xFF8D9099),
    outlineVariant = Color(0xFF43474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF2B1D00),
    secondary = Color(0xFF00658E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6E7FF),
    onSecondaryContainer = Color(0xFF001E2E),
    tertiary = Color(0xFFB2421C),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF1B1C1F),
    surface = Color(0xFFF7F7FA),
    onSurface = Color(0xFF1B1C1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F1F5),
    surfaceContainer = Color(0xFFEBEBF0),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFDFDFE4),
)

@Composable
fun RetroHallTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

val Platform.brandColor: Color get() = Color(color)
