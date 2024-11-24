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
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Single(createdAtStart = true)
class MediaDurationExtractor(
    mainScope: CoroutineScope,
    @Named(ExoplayerModule.UTILITY) lazyExoPlayer: Lazy<ExoPlayer>,
    audiobooksDao: AudiobooksDao
) {
    private val exoPlayer by lazyExoPlayer

    init {
        mainScope.launch {
            delay(5_000) // Don't block app startup.
            exoPlayer.playWhenReady = false
            audiobooksDao.getFilesWithoutDuration(10).onEach { files ->
                val durations = files.map { exoPlayer.extractDuration(it.uri) }
                if (durations.isNotEmpty()) {
                    exoPlayer.clearMediaItems()
                    audiobooksDao.insertAudiobookFileDurations(durations)
                }
            }.launchIn(mainScope)
        }
    }

    private suspend fun ExoPlayer.extractDuration(uri: Uri): AudiobookFileDuration =
        suspendCoroutine { continuation ->
            val listener = object : Player.Listener {
                private fun result(durationMs: Long) {
                    removeListener(this)
                    continuation.resume(AudiobookFileDuration(uri, durationMs))
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    result(AudiobookFileDuration.INVALID)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> result(this@extractDuration.duration)
                        Player.STATE_ENDED -> result(AudiobookFileDuration.INVALID)
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