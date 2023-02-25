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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    when (val currentViewState = viewState.value) {
        is PlayerViewModel.ViewState.Browse ->
            BooksPager(
                modifier = modifier.fillMaxSize(),
                itemPadding = DefaultSpacing.ScreenContentPadding,
                books = currentViewState.books,
                onPlay = viewModel::play,
                onPageChanged = viewModel::onPageChanged
            )
        is PlayerViewModel.ViewState.Playing ->
            Playback(
                landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
                modifier = modifier.fillMaxSize().padding(DefaultSpacing.ScreenContentPadding),
                progress = currentViewState.progress,
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
        is PlayerViewModel.ViewState.Initializing -> Unit
    }
}
