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

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.settings.DATASTORE_UI_SETTINGS
import com.studio4plus.homerplayer2.settings.UiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class MainActivityViewModel(
    appContext: Context,
    dpm: DevicePolicyManager,
    @Named(DATASTORE_APP_STATE) appState: DataStore<StoredAppState>,
    @Named(DATASTORE_UI_SETTINGS) uiSettings: DataStore<UiSettings>
) : ViewModel() {
    val viewState = appState.data.map {
        MainActivityViewState.Ready(!it.onboardingCompleted)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityViewState.Loading)

    val lockTask = uiSettings.data.map {
        it.fullKioskMode && dpm.isLockTaskPermitted(appContext.packageName)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}

sealed interface MainActivityViewState {
    object Loading : MainActivityViewState
    data class Ready(val needsOnboarding: Boolean) : MainActivityViewState
}