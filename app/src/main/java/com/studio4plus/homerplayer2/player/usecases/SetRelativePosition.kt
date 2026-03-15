/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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

import android.net.Uri
import androidx.room.withTransaction
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.audiobooks.currentPositionMs
import com.studio4plus.homerplayer2.audiobooks.totalDurationMs
import com.studio4plus.homerplayer2.player.PlaybackUiStateRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetRelativePositionForCurrentBook(
    private val db: AppDatabase,
    private val audiobooksDao: AudiobooksDao,
    private val playbackUiStateRepository: PlaybackUiStateRepository,
) {
    suspend operator fun invoke(changeByMs: Long) {
        val bookId = playbackUiStateRepository.lastSelectedBookId().first()
        db.withTransaction {
            val audiobook = audiobooksDao.getAudiobook(bookId).first()
            if (audiobook != null) {
                val newPositionMs =
                    with(audiobook) {
                        (currentPositionMs() + changeByMs).coerceIn(0, totalDurationMs())
                    }
                val position = audiobook.calculatePosition(newPositionMs)
                if (position != null) {
                    audiobooksDao.updatePlayPosition(
                        audiobook.playbackState?.isNew ?: true,
                        position.first,
                        position.second,
                    )
                }
            }
        }
    }

    private fun AudiobooksDao.AudiobookWithState.calculatePosition(
        positionMs: Long
    ): Pair<Uri, Long>? =
        files
            .runningFold(Pair(files.first(), 0L)) { position, file ->
                val totalSoFar: Long = position.second + (position.first.durationMs ?: 0L)
                Pair(file, totalSoFar)
            }
            .findLast { it.second <= positionMs }
            ?.let { it.first.uri to (positionMs - (it.second)) }
}
