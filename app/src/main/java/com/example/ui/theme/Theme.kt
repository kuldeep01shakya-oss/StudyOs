package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0E3A43),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E2254),
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = AccentEmerald,
    onTertiary = Color.Black,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = AccentRose,
    onError = Color.White
)

@Composable
fun StudyOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // StudyOS is designed natively around high-contrast dark OLED styling
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StudyOSTheme(darkTheme = true, content = content)
}
