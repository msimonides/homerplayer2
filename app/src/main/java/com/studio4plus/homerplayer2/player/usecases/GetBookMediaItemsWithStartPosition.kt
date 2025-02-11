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

package com.studio4plus.homerplayer2.player.usecases

import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.studio4plus.homerplayer2.settingsdata.PlaybackSettings
import com.studio4plus.homerplayer2.player.Audiobook
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory
class GetBookMediaItemsWithStartPosition(
    @Named(SettingsDataModule.PLAYBACK) private val settings: DataStore<PlaybackSettings>,
) {
    @OptIn(UnstableApi::class)
    suspend operator fun invoke(
        book: Audiobook,
    ): MediaItemsWithStartPosition {
        var startIndex: Int = 0
        var startPositionMs: Long = 0

        if (book.currentUri != null) {
            val currentUriIndex = book.uris.indexOfFirst { it == book.currentUri }
            if (currentUriIndex >= 0) {
                val rewindOnResumeMs = settings.data.first().rewindOnResumeSeconds * 1_000
                startPositionMs = (book.currentPositionMs - rewindOnResumeMs).coerceAtLeast(0)
                startIndex = currentUriIndex
            }
        }
        return MediaItemsWithStartPosition(book.toMediaItems(), startIndex, startPositionMs)
    }

    private fun Audiobook.toMediaItems() =
        uris.map {
            MediaItem.Builder()
                .setMediaId(it.toString())
                .setUri(it)
                .build()
        }
}