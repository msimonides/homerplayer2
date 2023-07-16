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

package com.studio4plus.homerplayer2.player.service

import android.os.PowerManager.WakeLock
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.studio4plus.homerplayer2.player.DATASTORE_PLAYBACK_SETTINGS
import com.studio4plus.homerplayer2.player.PlaybackSettings
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Named

private const val RESUME_AFTER_STOP_TIME_MS = 30_000L
private const val FADE_OUT_TIME_MS = 10_000L
private const val FADE_OUT_STEP_MS = 250L

@Factory
class SleepTimer(
    private val mainScope: CoroutineScope,
    @InjectedParam private val player: ExoPlayer,
    private val deviceMotionDetector: DeviceMotionDetector,
    private val clock: Clock,
    private val wakeLock: WakeLock,
    @Named(DATASTORE_PLAYBACK_SETTINGS) playbackSettings: DataStore<PlaybackSettings>
) : Player.Listener {

    private var motionDetectionJob: Job? = null
    private var sleepJob: Job? = null
    private var stoppedBySleepTimer = false

    private val isPlayingFlow = MutableStateFlow(false)

    private val sleepTimerFlow = combine(
        isPlayingFlow.withPrevious(false),
        playbackSettings.data.map { it.sleepTimerSeconds }
    ) { (wasPlaying, isPlaying), sleepTimerSeconds ->
        when {
            isPlaying && sleepTimerSeconds != 0 -> {
                stoppedBySleepTimer = false
                motionDetectionJob?.cancel()
                motionDetectionJob = deviceMotionDetector.motionType
                    .filter { it == DeviceMotionDetector.MotionType.SIGNIFICANT_MOTION }
                    .onEach { onSignificantMotion(sleepTimerSeconds) }
                    .launchIn(mainScope)
                if (!wasPlaying) {
                    extendSleepTimer(sleepTimerSeconds)
                }
            }
            !isPlaying -> {
                if (!stoppedBySleepTimer) {
                    sleepJob?.cancel()
                    motionDetectionJob?.cancel()
                }
            }
            else -> {
                sleepJob?.cancel()
                motionDetectionJob?.cancel()
            }
        }
        Unit
    }

    init {
        sleepTimerFlow.launchIn(mainScope)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        isPlayingFlow.value = isPlaying
    }

    private fun onSignificantMotion(sleepTimerSeconds: Int) {
        if (!player.isPlaying) {
            player.play()
        }
        extendSleepTimer(sleepTimerSeconds)
    }

    private fun extendSleepTimer(sleepTimerSeconds: Int) {
        sleepJob?.cancel()
        sleepJob = mainScope.launch {
            delay(sleepTimerSeconds * 1000L)
            fadeOutAndStop()
            // Prevent sleep to keep the delay accurate, it's short.
            wakeLock.acquire(RESUME_AFTER_STOP_TIME_MS)
            delay(RESUME_AFTER_STOP_TIME_MS)
            motionDetectionJob?.cancel()
        }
    }

    private suspend fun fadeOutAndStop() {
        try {
            val fadeStartTime = clock.elapsedRealTime()
            while (player.volume > 0f) {
                delay(FADE_OUT_STEP_MS)
                val elapsedTime = (clock.elapsedRealTime() - fadeStartTime)
                player.volume = (1f - elapsedTime.toFloat() / FADE_OUT_TIME_MS).coerceIn(0f, 1f)
            }
            stoppedBySleepTimer = true
            player.pause()
        } finally {
            player.volume = 1f
        }
    }

    private fun <T> Flow<T>.withPrevious(initialValue: T): Flow<Pair<T, T>> =
        scan(Pair(initialValue, initialValue)) { (_, previous), new ->
            Pair(previous, new)
        }
}
