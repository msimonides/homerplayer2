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

package com.studio4plus.homerplayer2.player

import android.net.Uri
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.audiobooks.currentPositionMs
import com.studio4plus.homerplayer2.audiobooks.totalDurationMs

data class Audiobook(
    val id: String,
    val displayName: String,
    val currentUri: Uri?,
    val currentPositionMs: Long,
    val uris: List<Uri>,
    val progress: Float,
)

fun AudiobooksDao.AudiobookWithState.toAudiobook(): Audiobook {
    val totalNonZeroDurationMs =
        totalDurationMs()?.takeIf { it > 0 }?.toFloat() ?: Float.MAX_VALUE
    return Audiobook(
        audiobook.id,
        audiobook.displayName,
        playbackState?.currentUri,
        playbackState?.currentPositionMs ?: 0,
        files.map { it.uri },
        progress = currentPositionMs().toFloat() / totalNonZeroDurationMs
    )
}