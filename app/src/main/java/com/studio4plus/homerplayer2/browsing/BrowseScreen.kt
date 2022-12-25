/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.browsing

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: BrowseViewModel = koinViewModel()
    val books = viewModel.audiobooks.collectAsStateWithLifecycle()

    BooksPager(
        modifier = modifier.fillMaxSize(),
        itemPadding = DefaultSpacing.ScreenContentPadding,
        bookNames = books.value.map { it.displayName },
        onPlay = {}
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BooksPager(
    modifier: Modifier = Modifier,
    itemPadding: Dp = Dp(0f),
    bookNames: List<String>,
    onPlay: (bookIndex: Int) -> Unit
) {
    val initialPage = Int.MAX_VALUE / 2
    HorizontalPager(
        count = Int.MAX_VALUE,
        state = rememberPagerState(initialPage)
    ) {
        val bookIndex = (currentPage - initialPage).floorMod(bookNames.size)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(itemPadding)
        ) {
            Text(
                text = bookNames.getOrNull(bookIndex) ?: "",
                modifier = Modifier
                    .weight(2f)
                    .align(Alignment.CenterHorizontally)
            )
            Button(
                onClick = { onPlay(bookIndex) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.playback_play_button_description),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}

@Preview()
@Composable
fun DefaultPreview() {
    BooksPager(
        bookNames = listOf("Hamlet", "Macbeth", "Romeo and Juliet"),
        itemPadding = DefaultSpacing.ScreenContentPadding,
        onPlay = {})
}