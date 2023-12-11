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
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.ui.theme.HomerTheme
import kotlin.math.roundToInt

@Composable
fun Playback(
    landscape: Boolean,
    modifier: Modifier = Modifier,
    progress: Float,
    playerActions: PlayerActions
) {
    val buttonStop: @Composable BoxScope.() -> Unit = {
        RoundIconButton(
            modifier = Modifier.align(Alignment.Center),
            iconImage = Icons.Rounded.Stop,
            iconContentDescription = stringResource(R.string.playback_stop_button_description),
            containerColor = HomerTheme.colors.controlStop,
            onClick = playerActions.onStop
        )
    }
    if (landscape) {
        HorizontalPlayback(modifier, progress, playerActions, buttonStop)
    } else {
        VerticalPlayback(modifier, progress, playerActions, buttonStop)
    }
}

@Composable
private fun VerticalPlayback(
    modifier: Modifier = Modifier,
    progress: Float,
    playerActions: PlayerActions,
    buttonStop: @Composable BoxScope.() -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        BookPageLayout(Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
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
            Box(content = buttonStop)
        }
        VerticalBookProgressIndicator(progress, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun HorizontalPlayback(
    modifier: Modifier = Modifier,
    progress: Float,
    playerActions: PlayerActions,
    buttonStop: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        BookPageLayout(modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
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
            Box(content = buttonStop)
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

private val dummyPlayerActions = PlayerActions({}, {}, {}, {}, {}, {}, {})

@Preview(widthDp = 300, heightDp = 450)
@Composable
private fun VerticalPlaybackPreviewShort() =
    HomerPlayer2Theme {
        Playback(landscape = false, progress = 0.3f, playerActions = dummyPlayerActions)
    }

@Preview(widthDp = 300, heightDp = 650)
@Composable
private fun VerticalPlaybackPreviewTall() =
    HomerPlayer2Theme {
        Playback(landscape = false, progress = 0.3f, playerActions = dummyPlayerActions)
    }

@Preview(widthDp = 800, heightDp = 600)
@Composable
private fun HorizontalPlaybackPreviewShort() =
    HomerPlayer2Theme {
        Playback(landscape = true, progress = 0.3f, playerActions = dummyPlayerActions)
    }

@Preview(widthDp = 900, heightDp = 300)
@Composable
private fun HorizontalPlaybackPreviewLong() =
    HomerPlayer2Theme {
        Playback(landscape = true, progress = 0.3f, playerActions = dummyPlayerActions)
    }