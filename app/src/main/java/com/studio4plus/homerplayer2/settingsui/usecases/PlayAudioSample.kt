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

package com.studio4plus.homerplayer2.settingsui.usecases

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule
import com.studio4plus.homerplayer2.player.usecases.GetBookMediaItemsWithStartPosition
import com.studio4plus.homerplayer2.player.usecases.GetSelectedBook
import kotlinx.coroutines.delay
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import kotlin.time.Duration

@Factory
class PlayAudioSample(
    @Named(ExoplayerModule.PLAYBACK) private val exoPlayer: ExoPlayer,
    private val getSelectedBook: GetSelectedBook,
    private val getBookMediaItemsWithStartPosition: GetBookMediaItemsWithStartPosition,
) {

    @OptIn(UnstableApi::class)
    suspend operator fun invoke(speed: Float, duration: Duration) {
        val bookId = getSelectedBook()
        if (bookId != null) {
            val mediaItemsWithStartPosition = getBookMediaItemsWithStartPosition(bookId)
            exoPlayer.setPlaybackSpeed(speed)
            with(mediaItemsWithStartPosition) {
                exoPlayer.setMediaItems(mediaItems)
                exoPlayer.seekTo(startIndex, startPositionMs)
            }
            exoPlayer.prepare()
            try {
                exoPlayer.playWhenReady = true
                delay(duration)
            } finally {
                exoPlayer.playWhenReady = false
            }
        }
    }

    fun shutdown() {
        exoPlayer.release()
    }
}