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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.utils.SentryHelper
import kotlin.math.roundToInt

private const val VERTICAL = 0
private const val HORIZONTAL = 1

@Composable
fun ControlButtonsLayout(
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

        if (buttonSizePx <= 0) {
            // Apparently this is being laid out in a very small space.
            // Report some details and don't place anything.
            SentryHelper.reportOnce("ControlButtonsLayout.measure") {
                IllegalStateException("Button size too small ${buttonSizePx}; constraints: ${constraints.maxWidth} x ${constraints.maxHeight}")
            }
            return@Layout layout(constraints.maxWidth, constraints.maxWidth) {}
        }

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
