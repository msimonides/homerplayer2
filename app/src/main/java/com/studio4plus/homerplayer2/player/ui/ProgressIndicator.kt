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

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val IndicatorWidth = 12.dp

@Composable
fun VerticalBookProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0)
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier
            .progressSemantics(progress)
            .fillMaxHeight()
            .width(IndicatorWidth)
    ) {
        val xOffset = size.width / 2
        val progressHeight = size.height * progress
        val progressY = size.height - progressHeight

        drawLine(color, Offset(xOffset, 0f), Offset(xOffset, progressY))
        drawLine(
            color,
            Offset(xOffset, progressY),
            Offset(xOffset, size.height),
            strokeWidth = size.width
        )
    }
}

@Composable
fun HorizontalBookProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0)
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier
            .progressSemantics(progress)
            .fillMaxWidth()
            .height(IndicatorWidth)
    ) {
        val yOffset = size.height / 2
        val progressWidth = size.width * progress

        drawLine(
            color,
            Offset(0f, yOffset),
            Offset(progressWidth, yOffset),
            strokeWidth = size.height
        )
        drawLine(color, Offset(progressWidth, yOffset), Offset(size.width, yOffset))
    }
}

@Preview
@Composable
fun VerticalBookProgressIndicatorPreview() {
    VerticalBookProgressIndicator(0.3f, modifier = Modifier.padding(16.dp))
}

@Preview
@Composable
fun HorizontalBookProgressIndicatorPreview() {
    HorizontalBookProgressIndicator(0.3f, modifier = Modifier.padding(16.dp))
}