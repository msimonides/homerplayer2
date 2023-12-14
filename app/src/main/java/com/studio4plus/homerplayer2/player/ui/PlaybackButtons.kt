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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.HomerTheme

data class PlayerActions(
    val onPlay: (bookIndex: Int) -> Unit,
    val onStop: () -> Unit,
    val onSeekForward: () -> Unit,
    val onSeekBack: () -> Unit,
    val onFastForward: () -> Unit,
    val onFastRewind: () -> Unit,
    val onVolumeUp: () -> Unit,
    val onVolumeDown: () -> Unit
) {
    companion object {
        // For previews
        val EMPTY = PlayerActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun RoundIconButton(
    modifier: Modifier = Modifier,
    iconImage: ImageVector,
    iconContentDescription: String?,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.aspectRatio(1f),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White // TODO: should this be configurable?
        )
    ) {
        Icon(
            iconImage,
            contentDescription = iconContentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FlatIconButton(
    modifier: Modifier,
    iconImage: ImageVector,
    iconContentDescription: String?,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(
            iconImage,
            contentDescription = iconContentDescription,
            modifier = Modifier.fillMaxSize(),
            tint = color
        )
    }
}

@Composable
fun ButtonVolumeUp(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.VolumeUp,
    iconContentDescription = stringResource(id = R.string.playback_volume_up_button_description),
    color = HomerTheme.colors.controlVolume,
    onClick = playerActions.onVolumeUp
)

@Composable
fun ButtonVolumeDown(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.VolumeDown,
    iconContentDescription = stringResource(id = R.string.playback_volume_down_button_description),
    color = HomerTheme.colors.controlVolume,
    onClick = playerActions.onVolumeDown
)

@Composable
fun ButtonFastRewind(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.FastRewind,
    iconContentDescription = stringResource(id = R.string.playback_fast_rewind_button_description),
    color = HomerTheme.colors.controlFast,
    onClick = playerActions.onFastRewind
)

@Composable
fun ButtonFastForward(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.FastForward,
    iconContentDescription = stringResource(id = R.string.playback_fast_forward_button_description),
    color = HomerTheme.colors.controlFast,
    onClick = playerActions.onFastForward
)

@Composable
fun ButtonSeekBack(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.Replay30,
    iconContentDescription = stringResource(id = R.string.playback_replay30_rewind_button_description),
    color = HomerTheme.colors.controlSeek,
    onClick = playerActions.onSeekBack
)

@Composable
fun ButtonSeekForward(
    modifier: Modifier,
    playerActions: PlayerActions
) = FlatIconButton(
    modifier = modifier,
    iconImage = Icons.Rounded.Forward10,
    iconContentDescription = stringResource(id = R.string.playback_forward10_button_description),
    color = HomerTheme.colors.controlSeek,
    onClick = playerActions.onSeekForward
)