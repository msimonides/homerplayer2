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

package com.studio4plus.homerplayer2.settingsui.composables

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R

class LayoutSizeDragHandleState(
    private val maxValue: Dp,
    initialValue: Dp,
) {

    private val rawValue = mutableFloatStateOf(initialValue.value)
    val valueDp = derivedStateOf { clampAndSnap(rawValue.floatValue).dp }

    fun updateBy(delta: Dp) {
        rawValue.floatValue += delta.value
    }

    fun updateToAndClamp(newValue: Dp) {
        rawValue.floatValue = clampAndSnap(newValue.value)
    }

    private fun clampAndSnap(newValue: Float): Float {
        val clampedValue = newValue.coerceIn(0f, maxValue.value)
        return if (clampedValue < SNAP_TO_ZERO_THRESHOLD) 0f else clampedValue
    }

    companion object {
        private const val SNAP_TO_ZERO_THRESHOLD = 16f

        val Saver = listSaver<LayoutSizeDragHandleState, Float>(
            save = { listOf(it.maxValue.value, it.rawValue.floatValue) },
            restore = { LayoutSizeDragHandleState(it[0].dp, it[1].dp)}
        )
    }
}

@Composable
fun rememberLayoutSizeDragHandleState(initialValue: Dp, maxValue: Dp) =
    rememberSaveable(maxValue, saver = LayoutSizeDragHandleState.Saver) {
        LayoutSizeDragHandleState(maxValue, initialValue)
    }

@Composable
fun LayoutSizeDragHandle(
    dragOrientation: Orientation,
    state: LayoutSizeDragHandleState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    reverse: Boolean = false,
    onDragChange: (Boolean) -> Unit = {}
) {
    val density = LocalDensity.current
    val size = when(dragOrientation) {
        Orientation.Vertical -> Modifier
            .size(width = Dp.Unspecified, height = 2.dp)
            .fillMaxWidth()
        Orientation.Horizontal -> Modifier
            .size(width = 2.dp, height = Dp.Unspecified)
            .fillMaxHeight()
    }
    Box(
        modifier = modifier.then(size)
    ) {
        val draggableState = rememberDraggableState { delta ->
            with(density) {
                state.updateBy((if (reverse) -delta else delta).toDp())
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .requiredSize(48.dp)
                .systemGestureExclusion { coordinates ->
                    with(coordinates.size) {
                        Rect(
                            left = -0.5f * width,
                            right = 1.5f * width,
                            top = -0.5f * height,
                            bottom = 1.5f * height,
                        )
                    }
                }
                .draggable(
                    startDragImmediately = true,
                    orientation = dragOrientation,
                    onDragStarted = { onDragChange(true) },
                    onDragStopped = { velocity ->
                        onDragChange(false)
                        draggableState.drag {
                            val decay = FloatExponentialDecaySpec(frictionMultiplier = 4f)
                            animateDecay(
                                state.valueDp.value.value,
                                if (reverse) -velocity else velocity,
                                decay
                            ) { value, _ ->
                                state.updateToAndClamp(value.dp)
                            }
                        }
                    },
                    state = draggableState
                )
                .align(Alignment.Center)
                .background(color)
                .padding(4.dp)
        ) {
            val iconRes = when(dragOrientation) {
                Orientation.Vertical -> R.drawable.icon_arrow_up_down
                Orientation.Horizontal -> R.drawable.icon_arrow_left_right
            }
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = contentColorFor(color)
            )
        }
    }
}