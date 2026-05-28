package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElegantDarkAccent,
    onPrimary = Color(0xFF003258),
    primaryContainer = ElegantDarkAccentSecondary,
    onPrimaryContainer = ElegantDarkAccent,
    secondary = ElegantDarkMuted,
    onSecondary = Color.Black,
    background = ElegantDarkBg,
    onBackground = ElegantDarkText,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkText,
    surfaceVariant = ElegantDarkSurfaceSubtle,
    onSurfaceVariant = ElegantDarkText,
    outline = ElegantDarkSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We always force elegant dark theme for consistency
    dynamicColor: Boolean = false, // Keep false to preserve exact designer specification
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
