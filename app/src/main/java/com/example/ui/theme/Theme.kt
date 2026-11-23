package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VaultDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    tertiary = IndigoTertiary,
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoOnContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer
)

private val VaultLightColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = LightEmeraldOnContainer,
    secondary = CyanSecondary,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = CrimsonError
)

private val VaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VaultShieldTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content
    )
}
