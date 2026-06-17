package com.example.slideshowclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    primaryContainer = AccentDim,
    onPrimaryContainer = TextHigh,
    background = Ink,
    onBackground = TextHigh,
    surface = Surface1,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    outline = Outline,
    outlineVariant = AccentDim,
)

@Composable
fun SlideshowClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
