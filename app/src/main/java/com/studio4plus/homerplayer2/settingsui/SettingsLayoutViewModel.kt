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

package com.studio4plus.homerplayer2.settingsui

import androidx.compose.ui.unit.Dp
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.settingsdata.PlayerLayoutMargins
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class SettingsLayoutViewModel(
    private val mainScope: CoroutineScope,
    @Named(SettingsDataModule.UI) private val uiSettingsStore: DataStore<UiSettings>
) : ViewModel() {

    val playerUiSettings = uiSettingsStore.data.map {
        it.playerUiSettings
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun save(
        portraitHorizontal: Dp,
        portraitBottom: Dp,
        landscapeHorizontal: Dp,
        landscapeBottom: Dp
    ) {
        mainScope.launchUpdate(uiSettingsStore) { uiSettings ->
            val playerLayout = uiSettings.playerUiSettings.layout.copy(
                portraitMargins = PlayerLayoutMargins(portraitHorizontal.value, portraitBottom.value),
                landscapeMargins = PlayerLayoutMargins(landscapeHorizontal.value, landscapeBottom.value)
            )
            uiSettings.copy(playerUiSettings = uiSettings.playerUiSettings.copy(layout = playerLayout))
        }
    }

}