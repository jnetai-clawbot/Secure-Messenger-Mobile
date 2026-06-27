package com.jnetaol.securemessenger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jnetaol.securemessenger.SecureMessengerApp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = DarkError,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    error = LightError,
    outline = LightOutline
)

@Composable
fun SecureMessengerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SecureMessengerApp
    val settings by app.settingsRepository.settings.collectAsState(initial = com.jnetaol.securemessenger.data.model.AppSettings())

    val customPrimary = Color(settings.primaryColor)
    val customBackground = Color(settings.backgroundColor)
    val customText = Color(settings.textColor)
    val customAccent = Color(settings.accentColor)

    val colorScheme = if (settings.isDarkMode) {
        DarkColorScheme.copy(
            primary = customPrimary,
            background = customBackground,
            onBackground = customText,
            surface = DarkSurface,
            onSurface = customText,
            surfaceVariant = DarkSurfaceVariant,
            error = DarkError,
            outline = DarkOutline
        )
    } else {
        LightColorScheme.copy(
            primary = customPrimary,
            background = customBackground,
            onBackground = customText,
            surface = LightSurface,
            onSurface = customText,
            surfaceVariant = LightSurfaceVariant,
            error = LightError,
            outline = LightOutline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = customBackground.toArgb()
            window.navigationBarColor = customBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !settings.isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
