package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = OrangeOnContainerDark,
    secondary = SlateBlueSecondary,
    onSecondary = CharcoalBackground,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = OrangeOnContainerDark,
    onTertiary = CharcoalBackground,
    tertiaryContainer = NavySurfaceElevated,
    onTertiaryContainer = CrispWhite,
    background = CharcoalBackground,
    onBackground = CrispWhite,
    surface = NavySurface,
    onSurface = CrispWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = NavyOutline,
    outlineVariant = NavyOutlineVariant,
    error = StatusRed,
    errorContainer = StatusRedContainerDark,
    onError = CrispWhite,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangeAccentLight,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OrangeOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = OrangeOnPrimary,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = OrangeAccentLight,
    onTertiary = OrangeOnPrimary,
    tertiaryContainer = LightSurfaceVariant,
    onTertiaryContainer = LightOnBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = StatusRedLight,
    errorContainer = StatusRedContainerLight,
    onError = OrangeOnPrimary,
    onErrorContainer = StatusRedLight
)

val RohanEditsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun RohanEditsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RohanEditsTypography,
        shapes = RohanEditsShapes,
        content = content
    )
}
