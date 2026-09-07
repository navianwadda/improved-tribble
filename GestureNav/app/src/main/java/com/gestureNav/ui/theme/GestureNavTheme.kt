package com.gestureNav.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B4FF),
    onPrimary = Color(0xFF003060),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE2E3EA),
    surface = Color(0xFF191C23),
    onSurface = Color(0xFFE2E3EA),
    surfaceVariant = Color(0xFF1E2130),
    onSurfaceVariant = Color(0xFF8D909E),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001947),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF1A1C22),
    surface = Color.White,
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFECEEF5),
    onSurfaceVariant = Color(0xFF5A5D6E),
    error = Color(0xFFDC2626)
)

@Composable
fun GestureNavTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
