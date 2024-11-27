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

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme

@Composable
fun PlayerScreenNoContent(
    modifier: Modifier = Modifier
) {
    // TODO: add a button to open content settings.
    PlayerScreenInfoPanel(
        iconVector = Icons.Default.MusicOff,
        titleRes = R.string.player_no_books_title,
        messageRes = R.string.player_no_books_message,
        modifier = modifier,
    )
}

@Composable
fun PlayerScreenNoContentPendingPodcasts(
    modifier: Modifier = Modifier
) {
    // TODO: add info about downloading only on WiFi (if relevant)
    PlayerScreenInfoPanel(
        iconVector = Icons.Default.Podcasts,
        titleRes = R.string.player_no_books_podcasts_title,
        messageRes = R.string.player_no_books_podcasts_message,
        modifier = modifier,
    )
}

@Composable
private fun PlayerScreenInfoPanel(
    iconVector: ImageVector,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
        Box(modifier) {
            Column(
                modifier = Modifier
                    .align(BiasAlignment(0f, -0.2f))
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(messageRes))
            }
        }
    }
}

@Preview
@Composable
private fun PreviewPlayerScreenNoContent() {
    HomerPlayer2Theme {
        PlayerScreenNoContent(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun PreviewPlayerScreenNoContentPendingAudiobooks() {
    HomerPlayer2Theme {
        PlayerScreenNoContentPendingPodcasts(
            modifier = Modifier.fillMaxSize()
        )
    }
}
