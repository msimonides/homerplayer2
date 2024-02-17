/*
 * MIT License
 *
 * Copyright (c) 2024 Marcin Simonides
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach

@Composable
fun VolumeChangeIndicator(
    volumeChangeEvent: SharedFlow<Float>,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(volumeChangeEvent) {
        volumeChangeEvent
            .onEach { volume = it }
            .drop(1)
            .collectLatest {
                isVisible = true
                delay(2_000)
                isVisible = false
            }
    }
    val hideOnClick = if (isVisible) {
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = { isVisible = false },
        )
    } else {
        Modifier
    }
    val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween())
    val animatedVolume by animateFloatAsState(targetValue = volume)
    Spacer(
        modifier = modifier
            .then(hideOnClick)
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(24.dp)
            .drawVolumeIndicator(animatedVolume, color = MaterialTheme.colorScheme.onSurface)

    )
}

private fun Modifier.drawVolumeIndicator(
    @FloatRange(0.0, 1.0) volume: Float,
    color: Color
): Modifier =
    drawWithCache {
        // Show a minimal value even if volume is 0.
        val indicatedVolume = 0.1f + 0.9f * volume
        val lowEndY = size.height
        val highEndY = size.height * (1f - indicatedVolume)
        // TODO(RTL): check if it should be reversed
        val lowEndX = 0
        val highEndX = size.width * indicatedVolume
        val path = Path().apply {
            moveTo(lowEndX.toFloat(), lowEndY)
            lineTo(highEndX, lowEndY)
            lineTo(highEndX, highEndY)
            close()
        }
        onDrawBehind {
            drawPath(path, color)
            drawPath(
                path,
                color,
                style = Stroke(10.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(10.dp.toPx()))
            )
        }
    }
