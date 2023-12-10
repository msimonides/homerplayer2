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

private const val VERTICAL = 0
private const val HORIZONTAL = 1

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
        PlaybackLayout(Modifier.weight(1f)) {
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
        PlaybackLayout(modifier = Modifier.weight(1f)) {
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

@Composable
private fun PlaybackLayout(
    modifier: Modifier = Modifier,
    largeButtonRatio: Float = 0.5f,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        require(measurables.size == 2) { "Requires exactly two children" }
        val isVertical = constraints.maxHeight > constraints.maxWidth
        val largeButtonSize =
            if (isVertical) minOf(constraints.maxWidth, (constraints.maxHeight * largeButtonRatio).roundToInt())
            else minOf(constraints.maxHeight, (constraints.maxWidth * largeButtonRatio).roundToInt())

        val placeableLargeButton = measurables[1].measure(
            Constraints(largeButtonSize, largeButtonSize, largeButtonSize, largeButtonSize)
        )
        val placeableRest = measurables[0].measure(
            if (isVertical) constraints.copy(maxHeight = constraints.maxHeight - largeButtonSize)
            else constraints.copy(maxWidth = constraints.maxWidth - largeButtonSize)
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeableRest.placeRelative(IntOffset(0, 0))
            if (isVertical) {
                val offset = IntOffset(
                    (constraints.maxWidth - largeButtonSize) / 2,
                    constraints.maxHeight - largeButtonSize
                )
                placeableLargeButton.placeRelative(offset)
            } else {
                val offset = IntOffset(
                    constraints.maxWidth - largeButtonSize,
                    (constraints.maxHeight - largeButtonSize) / 2
                )
                placeableLargeButton.placeRelative(offset)
            }
        }
    }
}

@Composable
private fun ControlButtonsLayout(
    verticals: @Composable () -> Unit,
    horizontals: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    buttonSpacing: Dp = 4.dp,
) {
    Layout(
        contents = listOf(verticals, horizontals),
        modifier = modifier
    ) { inputMeasurables, constraints ->
        check(intArrayOf(0, 2).contains(inputMeasurables[VERTICAL].size))
        check(intArrayOf(0, 2, 4).contains(inputMeasurables[HORIZONTAL].size))

        val isVertical = constraints.maxHeight > constraints.maxWidth

        val placeVerticalsHorizontally =
            inputMeasurables[VERTICAL].size == 2 && inputMeasurables[HORIZONTAL].size == 2 ||
                    isVertical && inputMeasurables[HORIZONTAL].isNotEmpty() ||
                    !isVertical && inputMeasurables[HORIZONTAL].isEmpty()
        val measurables = if (placeVerticalsHorizontally) {
            listOf(
                emptyList(),
                inputMeasurables[VERTICAL].asReversed() + inputMeasurables[HORIZONTAL]
            )
        } else {
            inputMeasurables
        }

        val horizontalsColumns = if (measurables[HORIZONTAL].isNotEmpty()) 2 else 0
        val verticalColumns = if (measurables[VERTICAL].isNotEmpty()) 1 else 0
        val columnCount = verticalColumns + horizontalsColumns
        val rowCount = maxOf(measurables[HORIZONTAL].size / 2, measurables[VERTICAL].size)

        val buttonSizePx: Int = minOf(
            (constraints.maxWidth - (columnCount - 1) * buttonSpacing.toPx()) / columnCount,
            (constraints.maxHeight - (rowCount - 1) * buttonSpacing.toPx()) / rowCount
        ).roundToInt()

        val buttonConstraints = constraints.copy(
            minWidth = buttonSizePx,
            maxWidth = buttonSizePx,
            minHeight = buttonSizePx,
            maxHeight = buttonSizePx
        )
        val placeables = measurables.map {
            it.map { measurable -> measurable.measure(buttonConstraints) }
        }

        val totalWidth =
            (columnCount * buttonSizePx + (columnCount - 1) * buttonSpacing.toPx()).roundToInt()
        val totalHeight =
            (rowCount * buttonSizePx + (rowCount - 1) * buttonSpacing.toPx()).roundToInt()

        val buttonStep = buttonSizePx + buttonSpacing.toPx().roundToInt()
        layout(totalWidth, totalHeight) {
            var y = 0
            placeables[HORIZONTAL].chunked(2) { (left, right) ->
                right.place(IntOffset(totalWidth - buttonSizePx, y))
                left.place(IntOffset(totalWidth - buttonStep - buttonSizePx, y))
                y += buttonStep
            }

            placeables[VERTICAL].chunked(2) { (top, bottom) ->
                top.place(IntOffset(0, 0))
                bottom.place(IntOffset(0, totalHeight - buttonSizePx))
            }
        }
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