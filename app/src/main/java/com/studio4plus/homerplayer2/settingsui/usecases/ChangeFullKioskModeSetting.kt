/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.settingsui.usecases

import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.fullkioskmode.KioskResumeScheduler
import com.studio4plus.homerplayer2.settingsdata.FullKioskModeSetting
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.settingsui.launchUpdate
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import kotlin.time.Duration.Companion.minutes

private val TEMPORARY_KIOSK_MODE_DISABLE_DURATION = 10.minutes

@Factory
class ChangeFullKioskModeSetting(
    private val mainScope: CoroutineScope,
    @Named(SettingsDataModule.UI) private val uiSettingsStore: DataStore<UiSettings>,
    private val clock: Clock,
    private val kioskResumeScheduler: KioskResumeScheduler
) {
    enum class FullKioskModeSetValue {
        Enable, Disable, DisableTemporarily
    }

    operator fun invoke(value: FullKioskModeSetValue) {
        mainScope.launchUpdate(uiSettingsStore) {
            val enableTimestamp = when (value) {
                FullKioskModeSetValue.Enable -> {
                    kioskResumeScheduler.cancel()
                    FullKioskModeSetting.ENABLED
                }
                FullKioskModeSetValue.Disable -> {
                    kioskResumeScheduler.cancel()
                    FullKioskModeSetting.DISABLED
                }
                FullKioskModeSetValue.DisableTemporarily -> {
                    val duration = TEMPORARY_KIOSK_MODE_DISABLE_DURATION
                    kioskResumeScheduler.schedule(duration)
                    clock.wallTime() + duration.inWholeMilliseconds
                }
            }
            it.copy(fullKioskModeEnableTimestamp = enableTimestamp)
        }
    }
}
