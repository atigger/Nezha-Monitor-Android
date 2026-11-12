package com.atigger.status.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = StatusBlueDark,
    secondary = StatusMintDark,
    tertiary = StatusBlueDark,
    background = StatusSurfaceDark,
    surface = Color(0xFF151D2B),
    surfaceContainer = Color(0xFF192233),
    surfaceContainerHigh = Color(0xFF1D283A),
    primaryContainer = Color(0xFF28335B),
    onPrimaryContainer = Color(0xFFDDE2FF),
    secondaryContainer = Color(0xFF143A30),
    onSecondaryContainer = StatusMintDark,
    outline = Color(0xFF8291A9),
    outlineVariant = Color(0xFF293447),
    error = Color(0xFFFF9B90),
    onPrimary = Color(0xFF0E1830),
    onBackground = Color(0xFFF2F6FF),
    onSurface = Color(0xFFF2F6FF),
    onSurfaceVariant = Color(0xFFAFC1DD)
)

private val LightColorScheme = lightColorScheme(
    primary = StatusBlue,
    secondary = StatusMint,
    tertiary = StatusBlue,
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEBEEF5),
    surfaceContainerHigh = Color(0xFFF0F2F8),
    primaryContainer = Color(0xFFE9ECFF),
    onPrimaryContainer = Color(0xFF3443B6),
    secondaryContainer = Color(0xFFE0F3EB),
    onSecondaryContainer = Color(0xFF11694F),
    outline = Color(0xFF78849A),
    outlineVariant = Color(0xFFE1E5EE),
    error = Color(0xFFBD4B43),
    onPrimary = Color.White,
    onBackground = StatusInk,
    onSurface = StatusInk,
    onSurfaceVariant = Color(0xFF596579)
)

@Composable
fun StatusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
