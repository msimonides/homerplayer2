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
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskAvailable
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskEnabled
import com.studio4plus.homerplayer2.settingsdata.FullKioskModeSetting
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Duration.Companion.minutes

private val TEMPORARY_KIOSK_MODE_DISABLE_DURATION = 10.minutes

@KoinViewModel
class SettingsLockdownViewModel(
    @Named(SettingsDataModule.UI) private val uiSettingsStore: DataStore<UiSettings>,
    private val mainScope: CoroutineScope,
    private val isFullKioskAvailable: IsFullKioskAvailable,
    private val clock: Clock,
    isFullKioskEnabled: IsFullKioskEnabled,
) : ViewModel() {

    enum class FullKioskModeSetValue {
        Enable, Disable, DisableTemporarily
    }

    class ViewState(
        val fullKioskMode: IsFullKioskEnabled.Value,
        val fullKioskModeAvailable: Boolean,
        val hideSettingsButton: Boolean,
        val showBattery: Boolean,
    )

    val viewState = combine(
        uiSettingsStore.data,
        isFullKioskEnabled()
    ) { uiSettings, fullKioskModeValue ->
        ViewState(
            fullKioskMode = fullKioskModeValue,
            fullKioskModeAvailable = isFullKioskAvailable(),
            hideSettingsButton = uiSettings.hideSettingsButton,
            showBattery = uiSettings.showBatteryIndicator,
        )
    }

    fun setFullKioskMode(value: FullKioskModeSetValue) {
        mainScope.launchUpdate(uiSettingsStore) {
            val enableTimestamp = when (value) {
                FullKioskModeSetValue.Enable -> FullKioskModeSetting.ENABLED
                FullKioskModeSetValue.Disable -> FullKioskModeSetting.DISABLED
                FullKioskModeSetValue.DisableTemporarily ->
                    clock.wallTime() + TEMPORARY_KIOSK_MODE_DISABLE_DURATION.inWholeMilliseconds
            }
            it.copy(fullKioskModeEnableTimestamp = enableTimestamp)
        }
    }

    fun setHideSettingsButton(isHidden: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(hideSettingsButton = isHidden) }
    }

    fun setShowBatteryIndicator(isShown: Boolean) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(showBatteryIndicator = isShown) }
    }
}