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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.settingsdata.PlayerUiSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksPager(
    modifier: Modifier = Modifier,
    itemPadding: Dp = 0.dp,
    state: PlayerViewModel.BooksState.Books,
    playerActions: PlayerActions,
    playerUiSettings: PlayerUiSettings,
    onPageChanged: (bookIndex: Int) -> Unit,
    landscape: Boolean
) {
    val books = state.books
    val insanePageCount = 10_000
    val zeroPage = insanePageCount / 2
    val getBookIndex = rememberBookIndexLambda(zeroPage, state)

    val pagerState = rememberPagerState(
        initialPage = zeroPage + state.selectedIndex,
        pageCount = { if (books.isEmpty()) 0 else insanePageCount }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { pageIndex ->
            onPageChanged(getBookIndex(pageIndex))
        }
    }
    LaunchedEffect(pagerState, state.selectedIndex) {
        // scrollToPage breaks the next tap gesture (user needs to tap twice to trigger buttons)
        // Avoid unnecessary calls to minimize the issue. TODO: report to Google.
        val currentSelectedIndex = getBookIndex(pagerState.currentPage)
        if (currentSelectedIndex != state.selectedIndex) {
            pagerState.scrollToPage(zeroPage + state.selectedIndex)
        }
    }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !state.isPlaying
    // TODO: set key
    ) { pageIndex ->
        val bookIndex = getBookIndex(pageIndex)
        val book = books[bookIndex]
        BookPage(
            index = bookIndex,
            displayName = book.displayName,
            progress = book.progress,
            isPlaying = state.isPlaying,
            playerActions = playerActions,
            playerUiSettings = playerUiSettings,
            landscape = landscape,
            modifier = modifier.padding(itemPadding)
        )
    }
}

@Composable
private fun rememberBookIndexLambda(
    zeroPage: Int,
    state: PlayerViewModel.BooksState.Books
): (pageIndex: Int) -> Int {
    val bookCountState = rememberUpdatedState(state.books.size)
    return remember(zeroPage, bookCountState) {
        { pageIndex: Int -> (pageIndex - zeroPage).floorMod(bookCountState.value) }
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
            state = PlayerViewModel.BooksState.Books(
                listOf(
                    PlayerViewModel.UiAudiobook("1", "Hamlet", 0.3f),
                    PlayerViewModel.UiAudiobook("2", "Macbeth", 0f),
                    PlayerViewModel.UiAudiobook("3", "Romeo and Juliet", 0.9f),
                ),
                selectedIndex = 1,
                isPlaying = false,
            ),
            itemPadding = HomerTheme.dimensions.screenContentPadding,
            landscape = false,
            playerActions = PlayerActions.EMPTY,
            playerUiSettings = PlayerUiSettings(true, true, true),
            onPageChanged = {}
        )
    }
}