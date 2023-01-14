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

package com.studio4plus.homerplayer2.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BooksPager(
    modifier: Modifier = Modifier,
    itemPadding: Dp = 0.dp,
    books: List<PlayerViewModel.AudiobookState>,
    onPlay: (bookIndex: Int) -> Unit
) {
    val initialPage = Int.MAX_VALUE / 2
    HorizontalPager(
        count = if (books.isEmpty()) 0 else Int.MAX_VALUE,
        state = rememberPagerState(initialPage)
    // TODO: set key
    ) { pageIndex ->
        val bookIndex = (pageIndex - initialPage).floorMod(books.size)
        val book = books[bookIndex]
        BookPage(
            displayName = book.displayName,
            progress = book.progress,
            onPlay = { onPlay(bookIndex) },
            landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
            modifier = modifier.padding(itemPadding)
        )
    }
}

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BooksPager(
        books = listOf(
            PlayerViewModel.AudiobookState("1", "Hamlet", 0.3f),
            PlayerViewModel.AudiobookState("2", "Macbeth", 0f),
            PlayerViewModel.AudiobookState("3", "Romeo and Juliet", 0.9f),
        ),
        itemPadding = DefaultSpacing.ScreenContentPadding,
        onPlay = {}
    )
}