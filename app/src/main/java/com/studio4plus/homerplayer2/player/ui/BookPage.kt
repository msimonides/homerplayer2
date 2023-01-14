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
import androidx.compose.material.icons.rounded.PlayArrow
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
fun BookPage(
    landscape: Boolean,
    displayName: String,
    progress: Float,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val button: @Composable BoxScope.() -> Unit ={
        Button(
            onClick = onPlay,
            modifier = Modifier
                .aspectRatio(1f)
                .align(Alignment.Center)
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.playback_play_button_description),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    if (landscape) {
        HorizontalBookPage(
            displayName = displayName,
            progress = progress,
            modifier = modifier,
            button = button
        )
    } else {
        VerticalBookPage(
            displayName = displayName,
            progress = progress,
            modifier = modifier,
            button = button
        )
    }
}

@Composable
private fun VerticalBookPage(
    displayName: String,
    progress: Float,
    modifier: Modifier = Modifier,
    button: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayName,
                modifier = Modifier
                    .weight(2f)
                    .align(Alignment.CenterHorizontally)
            )
            Box(Modifier.weight(1f).fillMaxSize(), content = button)
        }
        VerticalBookProgressIndicator(progress, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun HorizontalBookPage(
    displayName: String,
    progress: Float,
    modifier: Modifier = Modifier,
    button: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(Modifier.weight(1f)) {
            Text(
                text = displayName,
                modifier = Modifier
                    .weight(2f)
                    .align(Alignment.CenterVertically)
            )
            Box(Modifier.weight(1f).fillMaxSize(), content = button)
        }
        HorizontalBookProgressIndicator(progress, Modifier.padding(top = 8.dp))
    }
}

@Preview
@Composable
private fun VerticalBookPagePreview() =
    BookPage(
        landscape = false,
        displayName = "Macbeth",
        progress = 0.3f,
        onPlay = { },
        modifier = Modifier.padding(16.dp)
    )

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun HorizontalBookPagePreview() =
    BookPage(
        landscape = true,
        displayName = "Macbeth",
        progress = 0.3f,
        onPlay = { },
        modifier = Modifier.padding(16.dp)
    )