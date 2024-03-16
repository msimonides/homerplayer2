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
import com.studio4plus.homerplayer2.settingsdata.PlayerUiSettings
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class SettingsPlayerUiViewModel(
    private val mainScope: CoroutineScope,
    @Named(SettingsDataModule.PLAYBACK) private val playbackSettingsStore: DataStore<PlaybackSettings>,
    @Named(SettingsDataModule.UI) private val uiSettingStore: DataStore<UiSettings>,
) : ViewModel() {
    data class ViewState(
        val flipToStop: Boolean,
        val playerUiSettings: PlayerUiSettings,
    )

    val viewState = combine(
        playbackSettingsStore.data,
        uiSettingStore.data
    ) { playbackSettings, uiSettings ->
        ViewState(
            flipToStop = playbackSettings.flipToStop,
            playerUiSettings = uiSettings.playerUiSettings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFlipToStop(enabled: Boolean) {
        mainScope.launchUpdate(playbackSettingsStore) { it.copy(flipToStop = enabled) }
    }

    fun setShowVolumeControls(enabled: Boolean) {
        updatePlayerUiSettings { it.copy(showVolumeControls = enabled) }
    }

    fun setShowFfRewindControls(enabled: Boolean) {
        updatePlayerUiSettings { it.copy(showFfRewindControls = enabled) }
    }

    fun setShowSeekControls(enabled: Boolean) {
        updatePlayerUiSettings { it.copy(showSeekControls = enabled) }
    }

    private fun updatePlayerUiSettings(transform: (PlayerUiSettings) -> PlayerUiSettings) {
        mainScope.launchUpdate(uiSettingStore) { it.copy(playerUiSettings = transform(it.playerUiSettings)) }
    }
}