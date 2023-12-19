/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.ui.theme.HomerTheme

@Composable
fun BookPage(
    landscape: Boolean,
    displayName: String,
    progress: Float,
    isPlaying: Boolean,
    index: Int,
    playerActions: PlayerActions,
    modifier: Modifier = Modifier
) {
    val button: @Composable BoxScope.() -> Unit = if (isPlaying) {
        {
            RoundIconButton(
                modifier = Modifier.align(Alignment.Center),
                iconImage = Icons.Rounded.Stop,
                iconContentDescription = stringResource(R.string.playback_stop_button_description),
                containerColor = HomerTheme.colors.controlStop,
                onClick = playerActions.onStop
            )
        }
    } else {
        {
            RoundIconButton(
                modifier = Modifier.align(Alignment.Center),
                iconImage = Icons.Rounded.PlayArrow,
                iconContentDescription = stringResource(R.string.playback_play_button_description),
                containerColor = HomerTheme.colors.controlPlay,
                onClick = { playerActions.onPlay(index) }
            )
        }
    }
    val mainContent: @Composable BoxScope.() -> Unit = if (isPlaying) {
         {
            ControlButtonsLayout(
                {
                    ButtonVolumeUp(modifier = Modifier, playerActions = playerActions)
                    ButtonVolumeDown(modifier = Modifier, playerActions = playerActions)
                },
                {
                    ButtonFastRewind(modifier = Modifier, playerActions = playerActions)
                    ButtonFastForward(modifier = Modifier, playerActions = playerActions)
                    ButtonSeekBack(modifier = Modifier, playerActions = playerActions)
                    ButtonSeekForward(modifier = Modifier, playerActions = playerActions)
                }
            )
        }
    } else {
        {
            AutosizeText(
                text = displayName,
                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            )
        }
    }
    if (landscape) {
        HorizontalBookPage(
            mainContent = mainContent,
            button = button,
            progress = progress,
            modifier = modifier,
        )
    } else {
        VerticalBookPage(
            mainContent = mainContent,
            button = button,
            progress = progress,
            modifier = modifier
                .padding(top = HomerTheme.dimensions.mainScreenIconSize),
        )
    }
}

@Composable
private fun VerticalBookPage(
    mainContent: @Composable BoxScope.() -> Unit,
    button: @Composable BoxScope.() -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
    ) {
        BookPageLayout(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                content = mainContent
            )
            Box(
                content = button,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        VerticalBookProgressIndicator(progress, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun HorizontalBookPage(
    mainContent: @Composable BoxScope.() -> Unit,
    button: @Composable BoxScope.() -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        BookPageLayout(Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.Center, content = mainContent)
            Box(
                content = button,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

@Preview
@Composable
private fun VerticalBookPagePreview() =
    HomerPlayer2Theme {
        BookPage(
            landscape = false,
            index = 0,
            displayName = "Macbeth",
            progress = 0.3f,
            isPlaying = false,
            playerActions = PlayerActions.EMPTY,
            modifier = Modifier.padding(16.dp)
        )
    }

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun HorizontalBookPagePreview() =
    HomerPlayer2Theme {
        BookPage(
            landscape = true,
            index = 0,
            displayName = "Macbeth",
            progress = 0.3f,
            isPlaying = false,
            playerActions = PlayerActions.EMPTY,
            modifier = Modifier.padding(16.dp)
        )
    }