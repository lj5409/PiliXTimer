package com.pilix.timer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    background = Color(0xFF070A0F),
    onBackground = Color(0xFFE9EFF8),
    surface = Color(0xFF0D1219),
    onSurface = Color(0xFFE9EFF8),
    surfaceVariant = Color(0xFF161E29),
    onSurfaceVariant = Color(0xFF8C9AB0),
    outline = Color(0xFF273140),
    outlineVariant = Color(0xFF1A222D),
    error = Color(0xFFEF4444),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0C1420),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C1420),
    surfaceVariant = Color(0xFFE7EDF5),
    onSurfaceVariant = Color(0xFF5B6A7E),
    outline = Color(0xFFC9D4E2),
    outlineVariant = Color(0xFFDDE5EF),
    error = Color(0xFFDC2626),
)

@Composable
fun PiliXTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
