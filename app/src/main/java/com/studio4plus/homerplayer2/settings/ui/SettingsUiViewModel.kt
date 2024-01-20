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

import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.settings.DATASTORE_UI_SETTINGS
import com.studio4plus.homerplayer2.settings.UiSettings
import com.studio4plus.homerplayer2.settings.UiThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class SettingsUiViewModel(
    @Named(DATASTORE_UI_SETTINGS) private val uiSettingsStore: DataStore<UiSettings>,
    private val mainScope: CoroutineScope,
    private val vibrator: Vibrator?,
) : ViewModel() {

    class ViewState(
        val enableHapticFeedback: Boolean?,
        val fullKioskMode: Boolean,
        val hideSettingsButton: Boolean,
        val showBattery: Boolean,
        val uiMode: UiThemeMode,
    )

    val viewState = uiSettingsStore.data.map { uiSettings ->
        ViewState(
            enableHapticFeedback =
                uiSettings.enableHapticFeedback.takeIf { hapticFeedbackAvailable() },
            fullKioskMode = uiSettings.fullKioskMode,
            hideSettingsButton = uiSettings.hideSettingsButton,
            showBattery = uiSettings.showBatteryIndicator,
            uiMode = uiSettings.uiThemeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setFullKioskMode(isEnabled: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(fullKioskMode = isEnabled) }
    }

    fun setHapticFeedback(isEnabled: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(enableHapticFeedback = isEnabled) }
    }

    fun setHideSettingsButton(isHidden: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(hideSettingsButton = isHidden) }
    }

    fun setShowBatteryIndicator(isShown: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(showBatteryIndicator = isShown) }
    }

    fun setUiMode(newUiMode: UiThemeMode) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(uiThemeMode = newUiMode) }
    }

    private fun hapticFeedbackAvailable() = vibrator?.hasVibrator() == true
}