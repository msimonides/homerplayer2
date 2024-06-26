package com.studio4plus.homerplayer2.base.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.IllegalStateException

@Immutable
data class ExtendedColors(
    val controlPlay: Color,
    val controlStop: Color,
    val controlVolume: Color,
    val controlFast: Color,
    val controlSeek: Color,
    val batteryRegular: Color,
    val batteryLow: Color
)

@Immutable
data class Dimensions(
    val labelSpacing: Dp,
    val mainScreenButtonSize: Dp,
    val mainScreenIconSize: Dp,
    val progressIndicatorWidth: Dp,
    val screenContentPadding: Dp,
    val settingsRowMinHeight: Dp
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        controlPlay = Color.Unspecified,
        controlStop = Color.Unspecified,
        controlVolume = Color.Unspecified,
        controlFast = Color.Unspecified,
        controlSeek = Color.Unspecified,
        batteryRegular = Color.Unspecified,
        batteryLow = Color.Unspecified
    )
}

private val HomerGreen = Color(0xff23a45d)
private val RedStop = Color(0xffc31e1e)

private val DarkColorScheme = darkColorScheme(
    primary = HomerGreen,
    onPrimary = Color.White,
    background = Color.Black,
)

private val ExtendedDarkColors = ExtendedColors(
    controlPlay = HomerGreen,
    controlStop = RedStop,
    controlVolume = Color(0xffeeff00),
    controlFast = Color(0xff447df8),
    controlSeek = Color(0xffffffff),
    batteryRegular = HomerGreen,
    batteryLow = RedStop,
)

private val LightColorScheme = lightColorScheme(
    primary = HomerGreen,
    onPrimary = Color.White,
    background = Color.White,

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
    controlPlay = HomerGreen,
    controlStop = RedStop,
    controlVolume = Color(0xffc1b61a),
    controlFast = Color(0xff3566d0),
    controlSeek = Color(0xff000000),
    batteryRegular = HomerGreen,
    batteryLow = RedStop,
)

private val LocalDimensions = staticCompositionLocalOf {
    Dimensions(
        labelSpacing = Dp.Unspecified,
        mainScreenButtonSize = Dp.Unspecified,
        mainScreenIconSize = Dp.Unspecified,
        progressIndicatorWidth = Dp.Unspecified,
        screenContentPadding = Dp.Unspecified,
        settingsRowMinHeight = Dp.Unspecified
    )
}

private val RegularDimensions = Dimensions(
    labelSpacing = 16.dp,
    mainScreenButtonSize = 48.dp,
    mainScreenIconSize = 32.dp,
    progressIndicatorWidth = 8.dp,
    screenContentPadding = 16.dp,
    settingsRowMinHeight = 48.dp,
)

private val LargeScreenDimensions = Dimensions(
    labelSpacing = 16.dp,
    mainScreenButtonSize = 80.dp,
    mainScreenIconSize = 64.dp,
    progressIndicatorWidth = 16.dp,
    screenContentPadding = 24.dp,
    settingsRowMinHeight = 48.dp,
)

@Composable
fun HomerPlayer2Theme(
    darkTheme: Boolean = isNightMode(),
    largeScreen: Boolean = isLargeScreen(),
    setWindowColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val materialColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val homerColorScheme = if (darkTheme) ExtendedDarkColors else ExtendedLightColors
    val dimensions = if (largeScreen) LargeScreenDimensions else RegularDimensions

    val view = LocalView.current
    if (!view.isInEditMode && setWindowColors) {
        SideEffect {
            val window = (view.context.getActivity()).window
            val insetsControllerCompat = WindowInsetsControllerCompat(window, view)
            val backgroundColorArgb = materialColorScheme.background.toArgb()
            window.statusBarColor = backgroundColorArgb
            insetsControllerCompat.isAppearanceLightStatusBars = !darkTheme

            if (Build.VERSION.SDK_INT >= 26) {
                window.navigationBarColor = backgroundColorArgb
                insetsControllerCompat.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun isLargeScreen() =
    if (LocalInspectionMode.current) {
        true
    } else {
        calculateWindowSizeClass(LocalContext.current.getActivity()).let { windowSizeClass ->
            windowSizeClass.heightSizeClass == WindowHeightSizeClass.Expanded ||
                    windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        }
    }

object HomerTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
    val dimensions: Dimensions
        @Composable
        get() = LocalDimensions.current
}

private fun Context.getActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> throw IllegalStateException()
    }