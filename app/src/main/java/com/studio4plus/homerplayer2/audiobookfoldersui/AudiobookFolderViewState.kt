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

package com.studio4plus.homerplayer2.audiobookfoldersui

import android.net.Uri
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.SmallCircularProgressIndicator

data class AudiobookFolderViewState(
    val displayName: String,
    val uri: Uri,
    val bookCount: Int,
    val bookTitles: String,
    val firstBookTitle: String?,
    val isScanning: Boolean,
    val isSamplesFolder: Boolean,
)

@Composable
fun AudiobookFolderViewState.subLabel() = when (bookCount) {
    0 -> stringResource(R.string.audiobook_folder_no_books_found)
    else -> bookTitles
}

@Composable
fun AudiobookFolderViewState.subLabelContentDescription() = when (bookCount) {
    0 -> stringResource(R.string.audiobook_folder_no_books_found)
    else -> pluralStringResource(
        id = R.plurals.audiobook_folder_content_description,
        count = bookCount,
        bookCount,
        firstBookTitle!!
    )
}

@Composable
fun BoxScope.AudiobookFolderBadgeContent(
    item: AudiobookFolderViewState,
) {
    if (item.isScanning) {
        SmallCircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .clearAndSetSemantics {}
        )
    } else {
        Text(
            item.bookCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .clearAndSetSemantics {}
        )
    }
}