package com.example.chatferit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

private val DarkColorScheme = darkColorScheme(
    primary = DarkGray,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Gray,
    onTertiary = Color.White,

    background = Color.Black,
    onBackground = Color.White,

    surface = DarkGray,
    onSurface = Color.White,

    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),

    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,

    secondaryContainer = Color.Gray,
    onSecondaryContainer = Color.White,


)


private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryBlue,
    onPrimary = OnLightPrimary,
    secondary = LightSecondaryPurple,
    onSecondary = OnLightSecondary,
    tertiary = LightTertiaryGreen,
    onTertiary = OnLightTertiary,

    background = LightSystemBackground,
    onBackground = OnLightFill,

    surface = LightSystemSurface,
    onSurface = OnLightFill,

    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    error = LightErrorRed,
    onError = OnLightError,

    primaryContainer = LightContainerPrimary,
    onPrimaryContainer = LightPrimaryBlue,

    secondaryContainer = LightContainerSecondary,
    onSecondaryContainer = LightSecondaryPurple
)

@Composable
fun ChatFeritTheme(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val currentThemeSetting by themeViewModel.currentThemeSetting.collectAsState()
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val useDarkTheme = when (currentThemeSetting) {
        ThemeSetting.SYSTEM -> isSystemInDarkTheme
        ThemeSetting.DARK -> true
        ThemeSetting.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}