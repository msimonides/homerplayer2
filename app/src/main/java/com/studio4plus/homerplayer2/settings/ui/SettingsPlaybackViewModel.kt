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

package com.studio4plus.homerplayer2.settings.ui

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.settingsdata.PlaybackSettings
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SettingsPlaybackViewModel(
    private val mainScope: CoroutineScope,
    @Named(SettingsDataModule.PLAYBACK) private val playbackSettingsStore: DataStore<PlaybackSettings>,
    private val playAudioSample: PlayAudioSample,
) : ViewModel() {

    private var playSampleJob: Job? = null

    data class ViewState(
        val rewindOnResumeSeconds: Int,
        val sleepTimerSeconds: Int,
        val playbackSpeed: Float,
    )

    val viewState = playbackSettingsStore.data.map { playbackSettings ->
        ViewState(
            rewindOnResumeSeconds = playbackSettings.rewindOnResumeSeconds,
            sleepTimerSeconds = playbackSettings.sleepTimerSeconds,
            playbackSpeed = playbackSettings.playbackSpeed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override fun onCleared() {
        super.onCleared()
        playAudioSample.shutdown()
    }

    fun setPlaybackSpeed(speed: Float) {
        mainScope.launchUpdate(playbackSettingsStore) { it.copy(playbackSpeed = speed) }
    }

    fun playSample(speed: Float) {
        playSampleJob?.cancel()
        playSampleJob = viewModelScope.launch {
            playAudioSample(speed, 3.seconds)
        }
    }

    fun setRewindOnResumeSeconds(seconds: Int) {
        mainScope.launchUpdate(playbackSettingsStore) { it.copy(rewindOnResumeSeconds = seconds) }
    }

    fun setSleepTimerSeconds(seconds: Int) {
        mainScope.launchUpdate(playbackSettingsStore) { it.copy(sleepTimerSeconds = seconds) }
    }
}