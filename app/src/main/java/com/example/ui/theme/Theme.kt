package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = VioletPrimary,
    onPrimary = VioletOnPrimary,
    primaryContainer = VioletPrimaryContainer,
    onPrimaryContainer = VioletOnPrimaryContainer,
    secondary = VioletSecondary,
    onSecondary = VioletOnSecondary,
    secondaryContainer = VioletSecondaryContainer,
    onSecondaryContainer = VioletOnSecondaryContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoTertiaryContainer,
    onTertiaryContainer = IndigoOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = StatusRed,
    errorContainer = StatusRedContainerLight,
    onError = VioletOnPrimary,
    onErrorContainer = StatusRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkVioletPrimary,
    onPrimary = DarkVioletOnPrimary,
    primaryContainer = DarkVioletPrimaryContainer,
    onPrimaryContainer = DarkVioletOnPrimaryContainer,
    secondary = DarkVioletSecondary,
    onSecondary = DarkVioletOnSecondary,
    secondaryContainer = DarkVioletSecondaryContainer,
    onSecondaryContainer = DarkVioletOnSecondaryContainer,
    tertiary = DarkIndigoTertiary,
    onTertiary = DarkIndigoOnTertiary,
    tertiaryContainer = DarkIndigoTertiaryContainer,
    onTertiaryContainer = DarkIndigoOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = StatusRed,
    errorContainer = StatusRedContainerDark,
    onError = VioletOnPrimary,
    onErrorContainer = DarkOnSurface
)

val RohanEditsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RohanEditsTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RohanEditsTypography,
        shapes = RohanEditsShapes,
        content = content
    )
}
