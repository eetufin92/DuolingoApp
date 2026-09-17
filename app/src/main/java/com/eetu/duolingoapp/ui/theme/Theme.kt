package com.eetu.duolingoapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DuoGreen,
    secondary = DuoBlue,
    tertiary = DuoGold,
    background = DuoNight,
    surface = DuoNightSurface,
    onPrimary = DuoTextLight,
    onSecondary = DuoTextLight,
    onTertiary = DuoTextDark,
    onBackground = DuoTextLight,
    onSurface = DuoTextLight,
)

private val LightColorScheme = lightColorScheme(
    primary = DuoGreen,
    secondary = DuoBlue,
    tertiary = DuoGold,
    background = DuoLightBackground,
    surface = DuoLightSurface,
    onPrimary = DuoTextLight,
    onSecondary = DuoTextLight,
    onTertiary = DuoTextDark,
    onBackground = DuoTextDark,
    onSurface = DuoTextDark,
)

@Composable
fun DuolingoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
