package com.example.todoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DC),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B),
    secondaryContainer = Color(0xFFCDE8DE),
    tertiary = Color(0xFF426277),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFDBE5E0),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFF9EF2DC),
    secondary = Color(0xFFB1CCC3),
    secondaryContainer = Color(0xFF334B44),
    tertiary = Color(0xFFA9CCE4),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB),
)

@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TodoTypography,
        content = content,
    )
}
