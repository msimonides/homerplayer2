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

package com.studio4plus.homerplayer2.settings.ui

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderNamesFlow
import com.studio4plus.homerplayer2.audiobookfoldersui.joinToEllipsizedString
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class SettingsMainViewModel(
    private val mainScope: CoroutineScope,
    audiobookFolderNamesFlow: AudiobookFolderNamesFlow,
    @Named(SettingsDataModule.UI) private val uiSettingsStore: DataStore<UiSettings>
) : ViewModel() {

    class ViewState(
        val audiobookFolders: String?,
        val ttsEnabled: Boolean,
        val uiMode: UiThemeMode,
    )

    val viewState = combine(
        audiobookFolderNamesFlow,
        uiSettingsStore.data
    ) { folderNames, uiSettings ->
        ViewState(
            audiobookFolders = folderNames.takeIf { it.isNotEmpty() }?.joinToEllipsizedString(),
            ttsEnabled = uiSettings.readBookTitles,
            uiMode = uiSettings.uiThemeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setUiMode(newUiMode: UiThemeMode) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(uiThemeMode = newUiMode) }
    }
}