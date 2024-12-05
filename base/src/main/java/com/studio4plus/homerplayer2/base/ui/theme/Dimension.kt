/*
 * MIT License
 *
 * Copyright (c) 2024 Marcin Simonides
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.studio4plus.homerplayer2.base.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.window.layout.WindowMetricsCalculator

@Immutable
data class Dimensions(
    val labelSpacing: Dp,
    val mainScreenButtonSize: Dp,
    val mainScreenIconSize: Dp,
    val podcastSearchImageSize: Dp,
    val progressIndicatorWidth: Dp,
    val screenHorizPadding: Dp,
    val screenHorizExtraPadding: Dp, // Used for max width on large screens.
    val screenVertPadding: Dp,
    val settingsRowMinHeight: Dp
) {
    val screenHorizTotalPadding = screenHorizPadding + screenHorizExtraPadding
}

internal val LocalDimensions = staticCompositionLocalOf {
    Dimensions(
        labelSpacing = Dp.Unspecified,
        mainScreenButtonSize = Dp.Unspecified,
        mainScreenIconSize = Dp.Unspecified,
        podcastSearchImageSize = Dp.Unspecified,
        progressIndicatorWidth = Dp.Unspecified,
        screenHorizPadding = Dp.Unspecified,
        screenHorizExtraPadding = Dp.Unspecified,
        screenVertPadding = Dp.Unspecified,
        settingsRowMinHeight = Dp.Unspecified,
    )
}

private val MaxMainContentWidth = 800.dp

internal fun screenDimensions(windowWidth: Dp) =
    if (windowWidth.isSpecified && windowWidth >= MaxMainContentWidth) {
        largeScreenDimensions(windowWidth)
    } else {
        regularScreenDimensions(windowWidth)
    }

private fun largeScreenDimensions(windowWidth: Dp): Dimensions {
    val screenContentPadding = 24.dp
    val screenContentExtraPadding =
        (windowWidth - MaxMainContentWidth - screenContentPadding.times(2)).coerceAtLeast(0.dp) / 2
    return Dimensions(
        labelSpacing = 16.dp,
        mainScreenButtonSize = 80.dp,
        mainScreenIconSize = 64.dp,
        podcastSearchImageSize = 128.dp,
        progressIndicatorWidth = 16.dp,
        screenHorizPadding = 24.dp,
        screenHorizExtraPadding = screenContentExtraPadding,
        screenVertPadding = 16.dp,
        settingsRowMinHeight = 48.dp,
    )
}


private fun regularScreenDimensions(windowWidth: Dp) = Dimensions(
    labelSpacing = 16.dp,
    mainScreenButtonSize = 48.dp,
    mainScreenIconSize = 32.dp,
    podcastSearchImageSize = 96.dp,
    progressIndicatorWidth = 8.dp,
    screenHorizPadding = 16.dp,
    screenHorizExtraPadding = 0.dp,
    screenVertPadding = 16.dp,
    settingsRowMinHeight = 48.dp,
)

@Composable
internal fun windowWidth(): Dp =
    if (LocalInspectionMode.current) {
        LocalConfiguration.current.screenWidthDp.dp
    } else {
        androidWindowWidth()
    }

@Composable
internal fun androidWindowWidth(): Dp {
    // Based on calculateWindowSizeClass from material3.windowsizeclass
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val activity = LocalContext.current.getActivity()
    val density = LocalDensity.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
    return size.width
}

private fun Context.getActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> throw IllegalStateException()
    }