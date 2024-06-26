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

package com.studio4plus.homerplayer2.settingsdata

import androidx.datastore.core.DataMigration
import kotlinx.serialization.Serializable

@Serializable
enum class UiThemeMode {
    SYSTEM, LIGHT, DARK
}

object FullKioskModeSetting {
    const val ENABLED = 0L
    const val DISABLED = Long.MAX_VALUE
}

@Serializable
data class PlayerUiSettings(
    val showVolumeControls: Boolean = true,
    val showFfRewindControls: Boolean = true,
    val showSeekControls: Boolean = true,
) {
    val showAnyControls = showVolumeControls || showFfRewindControls || showSeekControls
}

@Serializable
data class UiSettings(
    @Deprecated("Use fullKioskModeEnableTimestamp") val fullKioskMode: Boolean? = null,

    val enableHapticFeedback: Boolean = false,
    val fullKioskModeEnableTimestamp: Long = migrate(fullKioskMode) ?: FullKioskModeSetting.DISABLED,
    val hideSettingsButton: Boolean = false,
    val playerUiSettings: PlayerUiSettings = PlayerUiSettings(),
    val readBookTitles: Boolean = false,
    val showBatteryIndicator: Boolean = false,
    val uiThemeMode: UiThemeMode = UiThemeMode.SYSTEM,
) {
    companion object {
        fun migrate(oldValue: Boolean?) = when (oldValue) {
            true -> FullKioskModeSetting.ENABLED
            false -> FullKioskModeSetting.DISABLED
            null -> null
        }
    }
}
