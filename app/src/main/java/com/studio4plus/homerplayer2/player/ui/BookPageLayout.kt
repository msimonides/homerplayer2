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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun BookPageLayout(
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
            Constraints(0, largeButtonSize, 0, largeButtonSize)
        )
        val placeableRest = measurables[0].measure(
            if (isVertical) {
                val maxHeight = constraints.maxHeight - largeButtonSize
                constraints.copy(minHeight = 0, maxHeight = maxHeight)
            } else {
                val maxWidth = constraints.maxWidth - largeButtonSize
                constraints.copy(minWidth = 0, maxWidth = maxWidth)
            }
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

@Preview(widthDp = 600, heightDp = 250)
@Composable
fun HorizontalBookPageLayoutPreview() {
    BookPageLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Red),
            content = {}
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Blue),
            content = {}
        )
    }
}