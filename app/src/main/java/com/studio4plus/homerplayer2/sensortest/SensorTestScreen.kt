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

package com.studio4plus.homerplayer2.sensortest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SensorTestScreen(viewModel: SensorTestViewModel = koinViewModel()) {

    val accelerator = viewModel.acceleration.collectAsStateWithLifecycle(initialValue = SensorTestViewModel.Acceleration(0f, 0f, 0f))
    val motionType = viewModel.motionType.collectAsStateWithLifecycle(
        initialValue = SensorTestViewModel.MotionType.OTHER
    )
    val background = when(motionType.value) {
        SensorTestViewModel.MotionType.FACE_DOWN -> Color(0x8000ff00)
        SensorTestViewModel.MotionType.SIGNIFICANT_MOTION -> Color(0x80ff0000)
        else -> Color.Transparent
    }
    Row(Modifier.fillMaxSize().padding(32.dp).background(background)) {
        with (accelerator.value) {
            ValuesDisplay(Modifier.weight(1f), ax, ay, az)
        }
    }
}

@Composable
fun ValuesDisplay(modifier: Modifier = Modifier, x: Float, y: Float, z: Float) {
    val numberFormat = remember { "%2.2f" }
    Column(modifier, horizontalAlignment = Alignment.End) {
        val style = MaterialTheme.typography.displayLarge
        Text(numberFormat.format(x), style = style)
        Text(numberFormat.format(y), style = style)
        Text(numberFormat.format(z), style = style)
    }
}

@Preview
@Composable
fun ValuesDisplayPreview() = ValuesDisplay(Modifier, 0.3f, 12.3232f, -32.3432f)