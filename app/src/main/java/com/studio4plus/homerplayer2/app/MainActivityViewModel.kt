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

package com.studio4plus.homerplayer2.app

import android.content.pm.ActivityInfo
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobookfolders.AudiobooksUpdater
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskEnabled
import com.studio4plus.homerplayer2.settingsdata.ScreenOrientation
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class MainActivityViewModel(
    @Named(DATASTORE_APP_STATE) appState: DataStore<StoredAppState>,
    isFullKioskEnabled: IsFullKioskEnabled,
    private val audiobooksUpdater: AudiobooksUpdater,
    @Named(SettingsDataModule.UI) uiSettingsStore: DataStore<UiSettings>,
) : ViewModel() {
    val viewState = appState.data.map {
        MainActivityViewState.Ready(!it.onboardingCompleted)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityViewState.Loading)

    val lockTask = isFullKioskEnabled()
        .map { it.isEnabledNow }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val screenOverloads = uiSettingsStore.data.map {
        when(it.screenOrientation) {
            ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_USER
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    fun onResume() {
        audiobooksUpdater.trigger()
    }
}

sealed interface MainActivityViewState {
    object Loading : MainActivityViewState
    data class Ready(val needsOnboarding: Boolean) : MainActivityViewState
}