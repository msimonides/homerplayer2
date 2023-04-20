package com.studio4plus.homerplayer2.ui.theme

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

@Immutable
data class ExtendedColors(
    val controlPlay: Color,
    val controlStop: Color,
    val controlVolume: Color,
    val controlFast: Color,
    val controlSeek: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        controlPlay = Color.Unspecified,
        controlStop = Color.Unspecified,
        controlVolume = Color.Unspecified,
        controlFast = Color.Unspecified,
        controlSeek = Color.Unspecified
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xff22b9a6),
    onPrimary = Color.White,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val ExtendedDarkColors = ExtendedColors(
    controlPlay = Color(0xff22b9a6),
    controlStop = Color(0xffc31e1e),
    controlVolume = Color(0xffeeff00),
    controlFast = Color(0xff1e62f7),
    controlSeek = Color(0xffffffff)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xff22b9a6),
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val ExtendedLightColors = ExtendedColors(
    controlPlay = Color(0xff22b9a6),
    controlStop = Color(0xffc31e1e),
    controlVolume = Color(0xffe1d41e),
    controlFast = Color(0xff2a55b3),
    controlSeek = Color(0xff000000)
)

@Composable
fun HomerPlayer2Theme(
    darkTheme: Boolean = isNightMode(),
    content: @Composable () -> Unit
) {
    val materialColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val homerColorScheme = if (darkTheme) ExtendedDarkColors else ExtendedLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = materialColorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides homerColorScheme) {
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
}