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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()
    val currentViewState = viewState.value

    when (currentViewState) {
        is PlayerViewModel.ViewState.Browse ->
            BooksPager(
                modifier = modifier.fillMaxSize(),
                itemPadding = DefaultSpacing.ScreenContentPadding,
                bookNames = currentViewState.books.map { it.displayName },
                onPlay = { index -> viewModel.play(currentViewState.books[index].id) }
            )
        is PlayerViewModel.ViewState.Playing ->
            Playback(
                modifier = modifier.fillMaxSize().padding(DefaultSpacing.ScreenContentPadding),
                onStop = { viewModel.stop() }
            )
        is PlayerViewModel.ViewState.Initializing -> Unit
    }
}

@Composable
fun Playback(modifier: Modifier = Modifier, onStop: () -> Unit) {
    Box(modifier = modifier) {
        Button(onClick = onStop, modifier = modifier.aspectRatio(1f)) {
            Icon(
                Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.playback_stop_button_description),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}