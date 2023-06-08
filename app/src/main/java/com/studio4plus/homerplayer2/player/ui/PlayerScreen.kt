/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.battery.BatteryIcon
import com.studio4plus.homerplayer2.battery.BatteryState
import com.studio4plus.homerplayer2.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    val batteryState = viewModel.batteryState.collectAsStateWithLifecycle().value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    Box(modifier.fillMaxSize()) {
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        when (viewState) {
            is PlayerViewModel.ViewState.Browse -> {
                BrowseBooks(
                    landscape = isLandscape,
                    modifier = Modifier.fillMaxSize(),
                    books = viewState.books,
                    initialSelectedIndex = viewState.initialSelectedIndex,
                    onPlay = viewModel::play,
                    onPageChanged = viewModel::onPageChanged,
                )
            }
            is PlayerViewModel.ViewState.Playing -> {
                Playback(
                    landscape = isLandscape,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(HomerTheme.dimensions.screenContentPadding),
                    progress = viewState.progress,
                    playerActions = PlayerActions(
                        onSeekForward = viewModel::seekForward,
                        onSeekBack = viewModel::seekBack,
                        onFastForward = viewModel::seekNext,
                        onFastRewind = viewModel::seekPrevious,
                        onStop = viewModel::stop,
                        onVolumeUp = viewModel::volumeUp,
                        onVolumeDown = viewModel::volumeDown
                    ),
                )
            }

            is PlayerViewModel.ViewState.Initializing -> Unit
        }

        val showSettingsButton = viewState is PlayerViewModel.ViewState.Browse
        val controlsRegularPadding = with(HomerTheme.dimensions) {
            (screenContentPadding - (mainScreenButtonSize - mainScreenIconSize)).coerceAtLeast(0.dp)
        }
        val portraitEndPadding =
            if (isLandscape) controlsRegularPadding
            else with(HomerTheme.dimensions) { 0.5 * progressIndicatorWidth + 2 * screenContentPadding }
        TopControlsRow(
            batteryState,
            showSettingsButton,
            onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = controlsRegularPadding, end = portraitEndPadding)
        )
    }
}

@Composable
private fun TopControlsRow(
    batteryState: BatteryState?,
    showSettingsButton: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        if (showSettingsButton) {
            SingleSettingsButton(
                onOpenSettings = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        }
        if (batteryState != null) {
            with(HomerTheme.dimensions) {
                BatteryIcon(
                    batteryState,
                    modifier = Modifier
                        .padding((mainScreenButtonSize - mainScreenIconSize) / 2)
                        .size(mainScreenIconSize)
                )
            }
        }
    }
}

// TODO: move the settings buttons to some separate composable
@Composable
private fun SingleSettingsButton(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(HomerTheme.dimensions.mainScreenButtonSize)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                modifier = Modifier.size(HomerTheme.dimensions.mainScreenIconSize)
            )
        }
    }
}

@Composable
private fun DoubleSettingsButton(
    isVisible: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainSettingsButtonInteractionSource = remember { MutableInteractionSource() }
    val isMainSettingsButtonPressed = mainSettingsButtonInteractionSource.collectIsPressedAsState()
    Box(modifier) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(HomerTheme.dimensions.mainScreenButtonSize)
                .align(Alignment.TopEnd),
            interactionSource = mainSettingsButtonInteractionSource
        ) {
            if (isVisible) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                    modifier = Modifier.size(HomerTheme.dimensions.mainScreenIconSize)
                )
            }
        }
        if (isMainSettingsButtonPressed.value) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(HomerTheme.dimensions.mainScreenButtonSize)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                    modifier = Modifier.size(HomerTheme.dimensions.mainScreenIconSize)
                )
            }
        }
    }
}
