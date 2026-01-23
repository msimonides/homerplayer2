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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.settingsdata.PlayerUiSettings

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookPage(
    landscape: Boolean,
    displayName: String,
    progress: Float,
    isPlaying: Boolean,
    index: Int,
    playerActions: PlayerActions,
    playerUiSettings: PlayerUiSettings,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
) {
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonColor by
        animateColorAsState(
            if (isPlaying) HomerTheme.colors.controlStop else HomerTheme.colors.controlPlay,
            animationSpec = tween(),
        )
    val button: @Composable (padding: PaddingValues) -> Unit = { padding ->
        val icon = if (isPlaying) R.drawable.icon_stop else R.drawable.icon_play_arrow
        val contentDescription =
            if (isPlaying) {
                stringResource(R.string.playback_stop_button_description)
            } else {
                stringResource(R.string.playback_play_button_description)
            }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(paddingValues = padding),
        ) {
            RoundIconButton(
                modifier = Modifier.align(Alignment.Center),
                icon = icon,
                iconContentDescription = contentDescription,
                containerColor = buttonColor,
                onClick = {
                    if (isPlaying) playerActions.onStop() else playerActions.onPlay(index)
                },
                interactionSource = buttonInteractionSource,
            )
        }
    }
    val mainContent: @Composable () -> Unit = {
        data class ContentState(val settings: PlayerUiSettings, val isPlaying: Boolean)
        AnimatedContent(ContentState(playerUiSettings, isPlaying)) {
            println("Animated content state: ${this}")
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                MainContent(
                    displayName = displayName,
                    pageIndex = index,
                    isPlaying = it.isPlaying,
                    playerUiSettings = it.settings,
                    playerActions = playerActions,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = this@AnimatedContent,
                )
            }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainContent(
    pageIndex: Int,
    displayName: String,
    isPlaying: Boolean,
    playerUiSettings: PlayerUiSettings,
    playerActions: PlayerActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
) {
    val showControls = isPlaying && playerUiSettings.showAnyControls
    with(sharedTransitionScope) {
        with(animatedContentScope) {
            @Composable
            fun Modifier.pageSharedElement(key: String) =
                sharedElement(rememberSharedContentState("$pageIndex - $key"), animatedContentScope)

            if (showControls) {
                ControlButtonsLayout(
                    modifier = modifier,
                    verticals = {
                        if (playerUiSettings.showVolumeControls) {
                            ButtonVolumeUp(
                                playerActions = playerActions,
                                modifier = Modifier.pageSharedElement("volumeUp"),
                            )
                            ButtonVolumeDown(
                                playerActions = playerActions,
                                modifier = Modifier.pageSharedElement("volumeDown"),
                            )
                        }
                    },
                    horizontals = {
                        if (playerUiSettings.showFfRewindControls) {
                            ButtonFastRewind(
                                playerActions = playerActions,
                                modifier =
                                    Modifier.pageSharedElement("fastRewind"),
                            )
                            ButtonFastForward(
                                playerActions = playerActions,
                                modifier = Modifier.pageSharedElement("fastForward"),
                            )
                        }
                        if (playerUiSettings.showSeekControls) {
                            ButtonSeekBack(
                                playerActions = playerActions,
                                modifier = Modifier.pageSharedElement("seekBack"),
                            )
                            ButtonSeekForward(
                                playerActions = playerActions,
                                modifier = Modifier.pageSharedElement("seekForward"),
                            )
                        }
                    },
                )
            } else {
                AutosizeText(
                    text = displayName,
                    style = LocalTextStyle.current,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.pageSharedElement("title"),
                )
            }
        }
    }
}

@Composable
private fun VerticalBookPage(
    mainContent: @Composable () -> Unit,
    button: @Composable (padding: PaddingValues) -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BookPageLayout(modifier = Modifier.weight(1f)) {
            mainContent()
            button(PaddingValues(vertical = 16.dp))
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun HorizontalBookPage(
    mainContent: @Composable () -> Unit,
    button: @Composable (padding: PaddingValues) -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BookPageLayout(Modifier.weight(1f)) {
            mainContent()
            button(PaddingValues(horizontal = 16.dp))
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

@Preview
@Composable
private fun VerticalBookPagePreview() =
    HomerPlayer2Theme {
        SharedTransitionLayout {
            BookPage(
                landscape = false,
                index = 0,
                displayName = "Macbeth",
                progress = 0.3f,
                isPlaying = false,
                playerActions = PlayerActions.EMPTY,
                playerUiSettings = PlayerUiSettings(true, true, true),
                sharedTransitionScope = this,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalBookPagePreview() =
    HomerPlayer2Theme {
        SharedTransitionLayout {
            BookPage(
                landscape = true,
                index = 0,
                displayName = "Macbeth",
                progress = 0.3f,
                isPlaying = false,
                playerActions = PlayerActions.EMPTY,
                playerUiSettings = PlayerUiSettings(true, true, true),
                sharedTransitionScope = this,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

@Preview
@Composable
private fun VerticalPlayingBookPagePreview() =
    HomerPlayer2Theme {
        SharedTransitionLayout {
            BookPage(
                landscape = false,
                index = 0,
                displayName = "Macbeth",
                progress = 0.3f,
                isPlaying = true,
                playerActions = PlayerActions.EMPTY,
                playerUiSettings = PlayerUiSettings(true, true, true),
                sharedTransitionScope = this,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun HorizontalPlayingBookPagePreview() =
    HomerPlayer2Theme {
        SharedTransitionLayout {
            BookPage(
                landscape = true,
                index = 0,
                displayName = "Macbeth",
                progress = 0.3f,
                isPlaying = true,
                playerActions = PlayerActions.EMPTY,
                playerUiSettings = PlayerUiSettings(false, false, false),
                sharedTransitionScope = this,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
