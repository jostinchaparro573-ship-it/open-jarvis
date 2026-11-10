package com.openjarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OpenJarvisDarkColors = darkColorScheme(
    primary = VoidColor.Violet,
    secondary = VoidColor.Cyan,
    tertiary = VoidColor.Green,
    background = VoidColor.Void950,
    surface = VoidColor.Void900,
    onPrimary = VoidColor.TextPrimary,
    onSecondary = VoidColor.Void950,
    onBackground = VoidColor.TextPrimary,
    onSurface = VoidColor.TextPrimary,
    error = VoidColor.Red
)

@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OpenJarvisDarkColors,
        content = content
    )
}
