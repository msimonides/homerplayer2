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

import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule
import com.studio4plus.homerplayer2.player.DATASTORE_PLAYBACK_SETTINGS
import com.studio4plus.homerplayer2.player.PlaybackSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class PlaybackService : MediaSessionService() {

    private val exoPlayer: ExoPlayer by inject(named(ExoplayerModule.PLAYBACK))
    private val deviceMotionDetector: DeviceMotionDetector by inject()
    private val mainScope: CoroutineScope by inject()
    private var mediaSession: MediaSession? = null
    private val playbackSettings: DataStore<PlaybackSettings> by inject(named(DATASTORE_PLAYBACK_SETTINGS))
    private val playerMediaSessionCallback: PlayerMediaSessionCallback by inject()
    private val playPositionUpdater: PlayPositionUpdater by inject()
    private val serviceScope: CoroutineScope =
        CoroutineScope(mainScope.coroutineContext + SupervisorJob())
    // Note: can scopes be used instead of parameters?
    private val sleepTimer: SleepTimer by inject { parametersOf(exoPlayer) }

    override fun onCreate() {
        super.onCreate()

        exoPlayer.addListener(playPositionUpdater)
        exoPlayer.addListener(PlayStopOnFaceDown(mainScope, exoPlayer, deviceMotionDetector))
        exoPlayer.addListener(sleepTimer)
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(playerMediaSessionCallback)
            .build()

        playbackSettings.data
            .map { it.playbackSpeed }
            .distinctUntilChanged()
            .onEach { exoPlayer.setPlaybackSpeed(it) }
            .launchIn(serviceScope)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

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