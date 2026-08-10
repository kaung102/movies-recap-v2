package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioColorScheme = darkColorScheme(
    primary = StudioGold,
    onPrimary = DarkCanvas,
    primaryContainer = StudioOrange,
    onPrimaryContainer = TextPrimary,
    secondary = StudioCyan,
    onSecondary = DarkCanvas,
    secondaryContainer = StudioPurple,
    onSecondaryContainer = TextPrimary,
    tertiary = StudioPink,
    onTertiary = TextPrimary,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MoviesRecapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
