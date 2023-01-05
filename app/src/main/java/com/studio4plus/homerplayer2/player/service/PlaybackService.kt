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

package com.studio4plus.homerplayer2.player.service

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future
import org.koin.android.ext.android.inject

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val mainScope: CoroutineScope by inject()
    private val audiobooksDao: AudiobooksDao by inject()

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback(mainScope, audiobooksDao))
            .build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // TODO: inject it with koin?
    private class MediaSessionCallback(
        private val mainScope: CoroutineScope,
        private val audiobooksDao: AudiobooksDao
    ) : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val id = mediaItems.first().mediaId
            return mainScope.future {
                val files = audiobooksDao.getFilesForBook(id)
                files?.map { MediaItem.fromUri(it.uri) }?.toMutableList()
                    ?: throw NoSuchElementException("No book with ID $id")
            }
        }
    }
}