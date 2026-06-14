package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicCyan,
    onPrimary = CosmicDarkBg,
    primaryContainer = CosmicBlue,
    onPrimaryContainer = CosmicTextPrimary,
    secondary = CosmicBlue,
    onSecondary = CosmicTextPrimary,
    tertiary = CosmicPurple,
    onTertiary = CosmicDarkBg,
    background = CosmicDarkBg,
    onBackground = CosmicTextPrimary,
    surface = CosmicCardBg,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicBorder,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force gorgeous dark mode experience by default for ARI!
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our cinematic styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
