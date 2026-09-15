package com.ugo.clip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HugoColorScheme = darkColorScheme(
    primary = HugoCyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = HugoVioletSecondary,
    onSecondary = Color(0xFF260058),
    secondaryContainer = Color(0xFF430094),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = HugoAmberWarning,
    onTertiary = Color(0xFF452B00),
    background = HugoObsidianBg,
    onBackground = HugoTextPrimary,
    surface = HugoSlateSurface,
    onSurface = HugoTextPrimary,
    surfaceVariant = HugoSlateCard,
    onSurfaceVariant = HugoTextSecondary,
    outline = HugoBorderStroke,
    error = HugoRedSentinel,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // HUGO Clip uses executive dark aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HugoColorScheme,
        typography = Typography,
        content = content
    )
}
