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

package com.studio4plus.homerplayer2.fullkioskmode

import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.lifecycle.IsInForeground
import com.studio4plus.homerplayer2.settingsdata.FullKioskModeSetting
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone

@Factory
class IsFullKioskEnabled(
    @Named(SettingsDataModule.UI) uiSettings: DataStore<UiSettings>,
    private val isFullKioskAvailable: IsFullKioskAvailable,
    isInForeground: IsInForeground,
    clock: Clock,
) {
    sealed class Value(val isEnabledNow: Boolean)
    object Enabled : Value(isEnabledNow = true)
    object Disabled : Value(isEnabledNow = false)
    data class DisabledUntil(val enableTime: LocalDateTime) : Value(isEnabledNow = false)

    private val settingsEnableTimestamp = uiSettings.data
        .map { it.fullKioskModeEnableTimestamp }
        .distinctUntilChanged()

    private val triggerFlow = combineTransform(
        isInForeground(),
        settingsEnableTimestamp
    ) { inForeground, enableTimestamp ->
        emit(clock.wallTime())
        val timeToEnableMs = enableTimestamp - clock.wallTime()
        if (inForeground && timeToEnableMs > 0 && enableTimestamp != FullKioskModeSetting.DISABLED) {
            delay(timeToEnableMs)
            emit(clock.wallTime())
        }
    }

    operator fun invoke(): Flow<Value> = combine(
        settingsEnableTimestamp, triggerFlow
    ) { enableTimestamp, time ->
        when {
            !isFullKioskAvailable() -> Disabled
            enableTimestamp == FullKioskModeSetting.DISABLED -> Disabled
            enableTimestamp <= time -> Enabled
            else -> {
                val enableTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(enableTimestamp),
                    TimeZone.getDefault().toZoneId()
                )
                DisabledUntil(enableTime)
            }
        }
    }.distinctUntilChanged()
}