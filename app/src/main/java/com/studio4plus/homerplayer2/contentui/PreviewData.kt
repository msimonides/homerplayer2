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

package com.studio4plus.homerplayer2.contentui

import android.net.Uri
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderViewState
import com.studio4plus.homerplayer2.audiobookfoldersui.joinToEllipsizedString
import com.studio4plus.homerplayer2.podcastsui.PodcastItemViewState
import java.time.LocalDate

object PreviewData {

    val folderItems1 = listOf(
        AudiobookFolderViewState(
            "Audiobooks",
            Uri.EMPTY,
            2,
            "Alice's Adventures in Wonderland, Hamlet",
            firstBookTitle = "Alice's Adventures in Wonderland",
            isScanning = false,
            isSamplesFolder = false,
        )
    )

    val folderItems50
        get() = (1..50).map { index ->
            val titles = (1..index).map { "Book $it" }
            AudiobookFolderViewState(
                "Folder $index",
                Uri.parse("dummy://$index"),
                titles.size,
                titles.joinToEllipsizedString(),
                firstBookTitle = titles.firstOrNull(),
                isScanning = false,
                isSamplesFolder = false,
            )
        }

    val podcasts1 = listOf(
        PodcastItemViewState("uri", "Podcast 1", LocalDate.of(2024, 1, 2))
    )
}