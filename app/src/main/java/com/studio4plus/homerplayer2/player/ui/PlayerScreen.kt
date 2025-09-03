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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.battery.BatteryIcon
import com.studio4plus.homerplayer2.battery.BatteryState
import com.studio4plus.homerplayer2.fullkioskmode.statusBarsFixedPadding
import com.studio4plus.homerplayer2.settingsdata.PlayerLayoutSettings
import com.studio4plus.homerplayer2.settingsdata.PlayerUiSettings
import com.studio4plus.homerplayer2.utils.max
import com.studio4plus.homerplayer2.utils.optional
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerRoute(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val bookState = viewModel.booksState.collectAsStateWithLifecycle().value
    val playerUiSettings = viewModel.playerUiSettings.collectAsStateWithLifecycle().value
    val batteryState = viewModel.batteryState.collectAsStateWithLifecycle().value
    val hideSettingsButton = viewModel.hideSettingsButton.collectAsStateWithLifecycle().value

    val playerActions = remember(viewModel) {
        PlayerActions(
            onPlay = viewModel::play,
            onStop = viewModel::stop,
            onSeekForward = viewModel::seekForward,
            onSeekBack = viewModel::seekBack,
            onFastForward = viewModel::seekNext,
            onFastRewind = viewModel::seekPrevious,
            onVolumeUp = viewModel::volumeUp,
            onVolumeDown = viewModel::volumeDown
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    if (playerUiSettings != null) {
        PlayerScreen(
            bookState,
            batteryState,
            hideSettingsButton,
            viewModel.volumeChangeEvent,
            playerActions,
            playerUiSettings,
            viewModel::onPageChanged,
            onOpenSettings,
            modifier.fillMaxSize()
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun PlayerScreen(
    booksState: PlayerViewModel.BooksState,
    batteryState: BatteryState?,
    hideSettingsButton: Boolean,
    volumeChangeEvent: SharedFlow<Float>,
    playerActions: PlayerActions,
    playerUiSettings: PlayerUiSettings,
    onPageChanged: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    PaddedPlayerBox(
        layoutSettings = playerUiSettings.layout,
        isLandscape = isLandscape,
        modifier = modifier
            .statusBarsFixedPadding()
            .optional(playerUiSettings.layout.hasMargins(isLandscape)) { background(Color.Black) }
            .navigationBarsPadding()

    ) {
        val contentModifier = Modifier.fillMaxSize()
        when (booksState) {
            is PlayerViewModel.BooksState.Books -> {
                BooksPager(
                    landscape = isLandscape,
                    modifier = contentModifier,
                    itemPadding = HomerTheme.dimensions.screenHorizPadding,
                    state = booksState,
                    playerActions = playerActions,
                    playerUiSettings = playerUiSettings,
                    onPageChanged = onPageChanged,
                )
            }

            PlayerViewModel.BooksState.NoBooksPendingPodcasts ->
                PlayerScreenNoContentPendingPodcasts(
                    contentModifier.padding(
                        horizontal = HomerTheme.dimensions.screenHorizPadding,
                        vertical = HomerTheme.dimensions.screenVertPadding
                    )
                )

            PlayerViewModel.BooksState.NoContent ->
                PlayerScreenNoContent(
                    contentModifier.padding(
                        horizontal = HomerTheme.dimensions.screenHorizPadding,
                        vertical = HomerTheme.dimensions.screenVertPadding
                    )
                )

            is PlayerViewModel.BooksState.Initializing -> Unit
        }

        val includeSettingsButton = booksState !is PlayerViewModel.BooksState.Initializing
        val controlsHorizontalPadding = with(HomerTheme.dimensions) {
            (screenHorizPadding - (mainScreenButtonSize - mainScreenIconSize) / 2).coerceAtLeast(0.dp)
        }
        val controlsPadding = PaddingValues(horizontal = controlsHorizontalPadding)
        val cutoutPadding = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal).asPaddingValues()
        TopControlsRow(
            batteryState,
            includeSettingsButton,
            hideSettingsButton,
            onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                .padding(max(controlsPadding, cutoutPadding))
        )
        val volumeChangePositioning = if (isLandscape) {
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = false)

        } else {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
        }
        VolumeChangeIndicator(
            volumeChangeEvent = volumeChangeEvent,
            modifier = volumeChangePositioning
        )
    }
}

@Composable
private fun TopControlsRow(
    batteryState: BatteryState?,
    includeSettingsButton: Boolean,
    hiddenSettingsMode: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        if (includeSettingsButton) {
            OpenSettingsButton(
                isHidden = hiddenSettingsMode,
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

private fun PlayerLayoutSettings.hasMargins(isLandscape: Boolean) = when {
    isLandscape -> landscapeMargins.bottom > 0f || landscapeMargins.horizontal > 0f
    else -> portraitMargins.bottom > 0f || portraitMargins.horizontal > 0f
}

@Preview
@Composable
private fun PreviewTopControlsLarge() {
    HomerPlayer2Theme(
        windowContentSize = DpSize(850.dp, 300.dp)
    ) {
        TopControlsRow(
            batteryState = BatteryState.Discharging(0.9f),
            includeSettingsButton = true,
            hiddenSettingsMode = false,
            onOpenSettings = {  },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
