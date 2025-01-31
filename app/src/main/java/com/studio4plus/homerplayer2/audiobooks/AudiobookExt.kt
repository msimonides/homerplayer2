/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.audiobooks

import android.net.Uri
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao.AudiobookWithState

fun AudiobookWithState.totalDurationMs(): Long? =
    if (files.all { it.durationMs != null }) files.sumOf { it.durationMs!! } else null

fun AudiobookWithState.currentPositionMs(): Long {
    val currentUri: Uri = playbackState?.currentUri ?: return 0
    val currentUriPositionMs = playbackState.currentPositionMs
    val previousFilesDuration =
        files.takeWhile { it.uri != currentUri }.sumOf { it.durationMs!! }
    return previousFilesDuration + currentUriPositionMs
}
