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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R

@Composable
fun Playback(
    landscape: Boolean,
    modifier: Modifier = Modifier,
    progress: Float,
    onStop: () -> Unit
) {
    val button: @Composable BoxScope.() -> Unit = {
        Button(
            onClick = onStop,
            modifier = Modifier
                .aspectRatio(1f)
                .align(Alignment.Center)
        ) {
            Icon(
                Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.playback_stop_button_description),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    if (landscape) {
        HorizontalPlayback(progress = progress, modifier = modifier, button = button)
    } else {
        VerticalPlayback(progress = progress, modifier = modifier, button = button)
    }
}

@Composable
private fun VerticalPlayback(
    modifier: Modifier = Modifier,
    progress: Float,
    button: @Composable BoxScope.() -> Unit
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            Text(
                "buttons here",
                modifier = Modifier
                    .weight(2f)
                    .align(Alignment.CenterHorizontally)
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                content = button
            )
        }
        VerticalBookProgressIndicator(progress, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun HorizontalPlayback(
    modifier: Modifier = Modifier,
    progress: Float,
    button: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.weight(1f)) {
            Text(
                "buttons here",
                modifier = Modifier
                    .weight(2f)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                content = button
            )
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

@Preview(widthDp = 300, heightDp = 450)
@Composable
private fun VerticalPlaybackPreview() =
    Playback(landscape = false, progress = 0.3f, onStop = {})

@Preview(widthDp = 450, heightDp = 300)
@Composable
private fun HorizontalPlaybackPreview() =
    Playback(landscape = true, progress = 0.3f, onStop = {})