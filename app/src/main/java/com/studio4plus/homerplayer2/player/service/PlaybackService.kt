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

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val mainScope: CoroutineScope by inject()
    private val audiobooksDao: AudiobooksDao by inject()
    private val exoPlayer: ExoPlayer by inject()
    private val deviceMotionDetector: DeviceMotionDetector by inject()

    override fun onCreate() {
        super.onCreate()

        exoPlayer.addListener(PlayPositionUpdater(mainScope, exoPlayer, audiobooksDao))
        exoPlayer.addListener(PlayStopOnFaceDown(mainScope, exoPlayer, deviceMotionDetector))
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(MediaSessionCallback())
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
    private class MediaSessionCallback : MediaSession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updateMediaItems = mediaItems.mapTo(mutableListOf()) {
                it.buildUpon().setUri(Uri.parse(it.mediaId)).build()
            }
            return Futures.immediateFuture(updateMediaItems)
        }
    }

    // TODO: inject it with koin?
    private class PlayPositionUpdater(
        private val mainScope: CoroutineScope,
        private val player: ExoPlayer,
        private val audiobooksDao: AudiobooksDao
    ) : Player.Listener {

        // TODO: handle changes via notification while paused.

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (!isPlaying) {
                val uri = player.currentMediaItem?.localConfiguration?.uri
                if (uri != null) {
                    mainScope.launch {
                        audiobooksDao.updatePlayPosition(uri, player.currentPosition)
                    }
                }
            }
        }
    }

    // TODO: inject it with koin?
    private class PlayStopOnFaceDown(
        private val mainScope: CoroutineScope,
        private val player: ExoPlayer,
        private val deviceMotionDetector: DeviceMotionDetector
    ) : Player.Listener {

        private var motionDetectionJob: Job? = null

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) {
                motionDetectionJob?.cancel()
                motionDetectionJob =
                    deviceMotionDetector.motionType
                        .filter { it == DeviceMotionDetector.MotionType.FACE_DOWN }
                        .onEach { player.stop() }
                        .launchIn(mainScope)
            } else {
                motionDetectionJob?.cancel()
            }
        }
    }
}