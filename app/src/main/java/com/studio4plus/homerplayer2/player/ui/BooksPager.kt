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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksPager(
    modifier: Modifier = Modifier,
    itemPadding: Dp = 0.dp,
    state: PlayerViewModel.ViewState.Books,
    playerActions: PlayerActions,
    onPageChanged: (bookIndex: Int) -> Unit,
    landscape: Boolean
) {
    val books = state.books
    val wrapPagesMargin = 1
    val zeroPage = wrapPagesMargin
    fun wrapPageIndex(pageIndex: Int) = when {
        pageIndex < zeroPage -> zeroPage + books.size - 1
        pageIndex >= zeroPage + books.size -> zeroPage
        else -> pageIndex
    }

    val pagerState = rememberPagerState(
        initialPage = zeroPage + state.initialSelectedIndex,
        pageCount = { if (books.isEmpty()) 0 else books.size + 2 * wrapPagesMargin }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.onEach { pageIndex ->
            val bookIndex = (pageIndex - zeroPage).floorMod(books.size)
            onPageChanged(bookIndex)
        }.launchIn(this)
        // Wrap to the other side of pager when page settles. It's a poor implementation but
        // until support for (Int.MAX_VALUE) page count is fixed it'll hae to do:
        // https://issuetracker.google.com/issues/313770354
        snapshotFlow { pagerState.settledPage }.onEach { pageIndex ->
            val wrapToPage = wrapPageIndex(pageIndex)
            if (wrapToPage != pageIndex)
                pagerState.scrollToPage(wrapToPage, pagerState.currentPageOffsetFraction)
        }.launchIn(this)
    }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !state.isPlaying
    // TODO: set key
    ) { pageIndex ->
        val bookIndex = (pageIndex - zeroPage).floorMod(books.size)
        val book = books[bookIndex]
        BookPage(
            index = bookIndex,
            displayName = book.displayName,
            progress = book.progress,
            isPlaying = state.isPlaying,
            playerActions = playerActions,
            landscape = landscape,
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
    HomerPlayer2Theme {
        BooksPager(
            state = PlayerViewModel.ViewState.Books(
                listOf(
                    PlayerViewModel.UiAudiobook("1", "Hamlet", 0.3f),
                    PlayerViewModel.UiAudiobook("2", "Macbeth", 0f),
                    PlayerViewModel.UiAudiobook("3", "Romeo and Juliet", 0.9f),
                ),
                initialSelectedIndex = 1,
                isPlaying = false,
            ),
            itemPadding = HomerTheme.dimensions.screenContentPadding,
            landscape = false,
            playerActions = PlayerActions.EMPTY,
            onPageChanged = {}
        )
    }
}