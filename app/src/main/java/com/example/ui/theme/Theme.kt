package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LcPlayerColorScheme = darkColorScheme(
    primary = TvPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = TvSurfaceVariant,
    onPrimaryContainer = TvSecondaryBlue,
    secondary = TvSecondaryBlue,
    onSecondary = Color.Black,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvSurfaceVariant,
    error = StatusErrorBorder,
    onError = Color.White
)

@Composable
fun LcPlayerTheme(
    content: @Composable () -> Unit
) {
    // LC Player is designed for Android TV and cinema viewing, consistently dark
    MaterialTheme(
        colorScheme = LcPlayerColorScheme,
        typography = Typography,
        content = content
    )
}
