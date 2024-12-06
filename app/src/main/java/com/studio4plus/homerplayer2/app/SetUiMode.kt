/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single(createdAtStart = true)
class SetUiMode(
    mainScope: CoroutineScope,
    appContext: Context,
    @Named(SettingsDataModule.UI) uiSettings: DataStore<UiSettings>
) {
    private val uiModeFlow = uiSettings.data.map {
        it.uiThemeMode
    }.distinctUntilChanged()

    init {
        uiModeFlow
            .onEach { uiMode ->
                if (Build.VERSION.SDK_INT >= 31) {
                    val uiModeManager =
                        appContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                    uiModeManager.setApplicationNightMode(uiMode.toApplicationNightMode())
                } else {
                    AppCompatDelegate.setDefaultNightMode(uiMode.toNightMode())
                }
            }.launchIn(mainScope)
    }

    private fun UiThemeMode.toNightMode() = when (this) {
        UiThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        UiThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        UiThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun UiThemeMode.toApplicationNightMode() = when (this) {
        UiThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        UiThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        UiThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
    }
}