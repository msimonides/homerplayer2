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

package com.studio4plus.homerplayer2.player.service

import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.studio4plus.homerplayer2.settingsdata.PlaybackSettings
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Named

@Factory
class StopOnFaceDown(
    mainScope: CoroutineScope,
    @InjectedParam private val player: ExoPlayer,
    private val deviceMotionDetector: DeviceMotionDetector,
    @Named(SettingsDataModule.PLAYBACK) playbackSettings: DataStore<PlaybackSettings>,
) : Player.Listener {

    private val isPlayingFlow = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val stopOnFaceDown = combine(
        isPlayingFlow,
        playbackSettings.data.map { it.flipToStop },
    ) { isPlaying, isEnabled ->
        isPlaying && isEnabled
    }.flatMapLatest { shouldStopOnFaceDown ->
        if (shouldStopOnFaceDown) {
            deviceMotionDetector.motionType
                .filter { it == DeviceMotionDetector.MotionType.FACE_DOWN }
                .onEach { player.stop() }
        } else {
            flowOf(Unit)
        }
    }

    init {
        stopOnFaceDown.launchIn(mainScope)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        isPlayingFlow.value = isPlaying
    }
}