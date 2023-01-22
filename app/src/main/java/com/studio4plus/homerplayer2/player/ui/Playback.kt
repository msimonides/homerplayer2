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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R

data class PlayerActions(
    val onSeekForward: () -> Unit,
    val onSeekBack: () -> Unit,
    val onFastForward: () -> Unit,
    val onFastRewind: () -> Unit,
    val onStop: () -> Unit,
    val onVolumeUp: () -> Unit,
    val onVolumeDown: () -> Unit
)

// TODO: implement RTL (media controls like ff and rewind should not be reversed in RTL).
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
        Column(Modifier.weight(1f)) {
            Row(
                Modifier
                    .weight(2f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                    ButtonVolumeDown(modifier = buttonModifier, playerActions = playerActions)
                    ButtonFastRewind(modifier = buttonModifier, playerActions = playerActions)
                    ButtonSeekBack(modifier = buttonModifier, playerActions = playerActions)
                }
                Column {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                    ButtonVolumeUp(modifier = buttonModifier, playerActions = playerActions)
                    ButtonFastForward(modifier = buttonModifier, playerActions = playerActions)
                    ButtonSeekForward(modifier = buttonModifier, playerActions = playerActions)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                content = buttonStop
            )
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
        Row(Modifier.weight(1f)) {
            Column(
                Modifier
                    .weight(2f)
                    .align(Alignment.CenterVertically)
            ) {
                Row {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                    ButtonVolumeUp(modifier = buttonModifier, playerActions = playerActions)
                    ButtonFastRewind(modifier = buttonModifier, playerActions = playerActions)
                    ButtonFastForward(modifier = buttonModifier, playerActions = playerActions)
                }
                Row {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                    ButtonVolumeDown(modifier = buttonModifier, playerActions = playerActions)
                    ButtonSeekBack(modifier = buttonModifier, playerActions = playerActions)
                    ButtonSeekForward(modifier = buttonModifier, playerActions = playerActions)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                content = buttonStop
            )
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun RoundIconButton(
    modifier: Modifier,
    iconImage: ImageVector,
    iconContentDescription: String?,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.aspectRatio(1f),
        onClick = onClick
    ) {
        Icon(
            iconImage,
            contentDescription = iconContentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// TODO: fix ripple size
@Composable
private fun FlatIconButton(
    modifier: Modifier,
    iconImage: ImageVector,
    iconContentDescription: String?,
    onClick: () -> Unit
) = IconButton(modifier = modifier.aspectRatio(1f), onClick = onClick) {
    Icon(
        iconImage,
        contentDescription = iconContentDescription,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ButtonVolumeUp(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.VolumeUp,
    iconContentDescription = stringResource(id = R.string.playback_volume_up_button_description),
    onClick = playerActions.onVolumeUp
)

@Composable
private fun ButtonVolumeDown(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.VolumeDown,
    iconContentDescription = stringResource(id = R.string.playback_volume_down_button_description),
    onClick = playerActions.onVolumeDown
)

@Composable
private fun ButtonFastRewind(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.FastRewind,
    iconContentDescription = stringResource(id = R.string.playback_fast_rewind_button_description),
    onClick = playerActions.onFastRewind
)

@Composable
private fun ButtonFastForward(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.FastForward,
    iconContentDescription = stringResource(id = R.string.playback_fast_forward_button_description),
    onClick = playerActions.onFastForward
)

@Composable
private fun ButtonSeekBack(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.Replay30,
    iconContentDescription = stringResource(id = R.string.playback_replay30_rewind_button_description),
    onClick = playerActions.onSeekBack
)

@Composable
private fun ButtonSeekForward(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.Forward10,
    iconContentDescription = stringResource(id = R.string.playback_forward10_button_description),
    onClick = playerActions.onSeekForward
)

private val dummyPlayerActions = PlayerActions({}, {}, {}, {}, {}, {}, {})

@Preview(widthDp = 300, heightDp = 450)
@Composable
private fun VerticalPlaybackPreviewShort() =
    Playback(landscape = false, progress = 0.3f, playerActions = dummyPlayerActions)

@Preview(widthDp = 300, heightDp = 650)
@Composable
private fun VerticalPlaybackPreviewTall() =
    Playback(landscape = false, progress = 0.3f, playerActions = dummyPlayerActions)

@Preview(widthDp = 800, heightDp = 600)
@Composable
private fun HorizontalPlaybackPreviewShort() =
    Playback(landscape = true, progress = 0.3f, playerActions = dummyPlayerActions)

@Preview(widthDp = 900, heightDp = 300)
@Composable
private fun HorizontalPlaybackPreviewLong() =
    Playback(landscape = true, progress = 0.3f, playerActions = dummyPlayerActions)