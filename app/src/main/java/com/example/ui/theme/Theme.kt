package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryBlue,
    secondary = SleekSecondaryBlue,
    tertiary = SleekTertiaryBlue,
    background = SleekDarkBackground,
    surface = SleekDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = SleekTextDark,
    onSurface = SleekTextDark,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekTextDarkMuted
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimaryBlue,
    secondary = SleekSecondaryBlue,
    tertiary = SleekTertiaryBlue,
    background = SleekLightBackground,
    surface = SleekLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SleekTextLight,
    onSurface = SleekTextLight,
    surfaceVariant = SleekLightSurfaceVariant,
    onSurfaceVariant = SleekTextLightMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
