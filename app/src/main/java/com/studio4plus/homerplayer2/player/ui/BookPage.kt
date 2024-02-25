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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme

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
    // TODO: wrap the button state animation into an Indication with IndicationNodeFactory once the
    //  API is released in future Compose versions.
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isPressed by buttonInteractionSource.collectIsPressedAsState()
    var isClicked by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isClicked -> 1.15f
            isPressed -> 1.1f
            else -> 1f
        },
        finishedListener = {
            if (isClicked) isClicked = false
        }
    )
    val scaleModifier = Modifier.graphicsLayer {scaleX = buttonScale; scaleY = buttonScale }
    fun animateClick() {
        isClicked = true
    }
    val buttonColor by animateColorAsState(
        if (isPlaying) HomerTheme.colors.controlStop else HomerTheme.colors.controlPlay,
        animationSpec = tween()
    )
    val button: @Composable BoxScope.() -> Unit = if (isPlaying) {
        {
            RoundIconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(scaleModifier),
                iconImage = Icons.Rounded.Stop,
                iconContentDescription = stringResource(R.string.playback_stop_button_description),
                containerColor = buttonColor,
                onClick = { animateClick(); playerActions.onStop() },
                interactionSource = buttonInteractionSource,
            )
        }
    } else {
        {
            RoundIconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(scaleModifier),
                iconImage = Icons.Rounded.PlayArrow,
                iconContentDescription = stringResource(R.string.playback_play_button_description),
                containerColor = buttonColor,
                onClick = { animateClick(); playerActions.onPlay(index) },
                interactionSource = buttonInteractionSource,
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
            modifier = modifier,
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
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        BookPageLayout(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                content = mainContent,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                contentAlignment = Alignment.Center,
                content = button,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            )
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 4.dp))
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
        BookPageLayout(
            Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                content = mainContent,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                contentAlignment = Alignment.Center,
                content = button,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
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

@Preview(widthDp = 800, heightDp = 300)
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

@Preview
@Composable
private fun VerticalPlayingBookPagePreview() =
    HomerPlayer2Theme {
        BookPage(
            landscape = false,
            index = 0,
            displayName = "Macbeth",
            progress = 0.3f,
            isPlaying = true,
            playerActions = PlayerActions.EMPTY,
            modifier = Modifier.padding(16.dp)
        )
    }

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalPlayingBookPagePreview() =
    HomerPlayer2Theme {
        BookPage(
            landscape = true,
            index = 0,
            displayName = "Macbeth",
            progress = 0.3f,
            isPlaying = true,
            playerActions = PlayerActions.EMPTY,
            modifier = Modifier.padding(16.dp)
        )
    }