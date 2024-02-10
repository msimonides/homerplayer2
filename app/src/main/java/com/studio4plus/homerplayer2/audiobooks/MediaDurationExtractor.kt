/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Single(createdAtStart = true)
class MediaDurationExtractor(
    mainScope: CoroutineScope,
    exoPlayer: ExoPlayer,
    audiobooksDao: AudiobooksDao
) {
    init {
        exoPlayer.playWhenReady = false
        audiobooksDao.getFilesWithoutDuration(10).onEach { files ->
            val durations = files.map { exoPlayer.extractDuration(it.uri) }
            if (durations.isNotEmpty()) {
                exoPlayer.clearMediaItems()
                audiobooksDao.insertAudiobookFileDurations(durations)
            }
        }.launchIn(mainScope)
    }

    private suspend fun ExoPlayer.extractDuration(uri: Uri): AudiobookFileDuration =
        suspendCoroutine { continuation ->
            fun result(durationMs: Long) = AudiobookFileDuration(uri, durationMs)

            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    removeListener(this)
                    continuation.resume(result(AudiobookFileDuration.INVALID))
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                            removeListener(this)
                            continuation.resume(result(this@extractDuration.duration))
                        }

                        Player.STATE_ENDED -> continuation.resume(result(AudiobookFileDuration.INVALID))
                        Player.STATE_BUFFERING -> Unit
                        Player.STATE_IDLE -> Unit
                    }
                }
            }
            addListener(listener)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
}