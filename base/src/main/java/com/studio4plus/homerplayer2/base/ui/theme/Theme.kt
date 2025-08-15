package com.studio4plus.homerplayer2.base.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class ExtendedColors(
    val controlPlay: Color,
    val controlStop: Color,
    val controlVolume: Color,
    val controlFast: Color,
    val controlSeek: Color,
    val batteryRegular: Color,
    val batteryLow: Color,
    val batteryCritical: Color
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        controlPlay = Color.Unspecified,
        controlStop = Color.Unspecified,
        controlVolume = Color.Unspecified,
        controlFast = Color.Unspecified,
        controlSeek = Color.Unspecified,
        batteryRegular = Color.Unspecified,
        batteryLow = Color.Unspecified,
        batteryCritical = Color.Unspecified
    )
}

private val HomerGreen = Color(0xff23a45d)
private val RedStop = Color(0xffc31e1e)

private val YellowDarkMode = Color(0xffeeff00)
private val YellowLightMode = Color(0xffc1b61a)

private val DarkColorScheme = darkColorScheme(
    primary = HomerGreen,
    onPrimary = Color.White,//onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val ExtendedDarkColors = ExtendedColors(
    controlPlay = HomerGreen,
    controlStop = RedStop,
    controlVolume = YellowDarkMode,
    controlFast = Color(0xff447df8),
    controlSeek = Color(0xffffffff),
    batteryRegular = HomerGreen,
    batteryLow = YellowDarkMode,
    batteryCritical = RedStop,
)

private val LightColorScheme = lightColorScheme(
    primary = HomerGreen,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = Color.White, //backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val ExtendedLightColors = ExtendedColors(
    controlPlay = HomerGreen,
    controlStop = RedStop,
    controlVolume = YellowLightMode,
    controlFast = Color(0xff3566d0),
    controlSeek = Color(0xff000000),
    batteryRegular = HomerGreen,
    batteryLow = YellowLightMode,
    batteryCritical = RedStop,
)

@Composable
fun HomerPlayer2Theme(
    darkTheme: Boolean = isNightMode(),
    screenLargeWidth: Dp = windowLargeWidth(),
    content: @Composable () -> Unit
) {
    val materialColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val homerColorScheme = if (darkTheme) ExtendedDarkColors else ExtendedLightColors
    val dimensions = screenDimensions(screenLargeWidth)

    CompositionLocalProvider(
        LocalExtendedColors provides homerColorScheme,
        LocalDimensions provides dimensions
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
private fun isNightMode() = when (AppCompatDelegate.getDefaultNightMode()) {
    AppCompatDelegate.MODE_NIGHT_NO -> false
    AppCompatDelegate.MODE_NIGHT_YES -> true
    else -> isSystemInDarkTheme()
}

object HomerTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
    val dimensions: Dimensions
        @Composable
        get() = LocalDimensions.current
}

