package com.cleancleaner.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002029),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = NeonEmerald,
    onSecondary = Color(0xFF003314),
    secondaryContainer = Color(0xFF005423),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = ElectricViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF371E7A),
    onTertiaryContainer = Color(0xFFEDE7F6),
    error = CoralRed,
    onError = Color.White,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    surfaceContainerHigh = DarkSurfaceContainerHigh
)

@Composable
fun CleanCleanerTheme(
    content: @Composable () -> Unit
) {
    // Pure dark theme default for snappy, minimal, battery-saving aesthetics
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
