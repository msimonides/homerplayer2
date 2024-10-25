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


import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class SettingsMainViewModel(
    private val appContext: Context,
    private val mainScope: CoroutineScope,
    audiobookFolderNamesFlow: AudiobookFolderNamesFlow,
    @Named(SettingsDataModule.UI) private val uiSettingsStore: DataStore<UiSettings>
) : ViewModel() {

    class ViewState(
        val audiobookFolders: String?,
        val rateAppIntent: Intent?,
        val ttsEnabled: Boolean,
        val uiMode: UiThemeMode,
    )

    private val rateAppIntent = flow {
        val intent = playStorePageIntent()
        emit(intent.takeIf { it.resolveActivity(appContext.packageManager) != null })
    }

    val viewState = combine(
        audiobookFolderNamesFlow,
        uiSettingsStore.data,
        rateAppIntent,
    ) { folderNames, uiSettings, rateAppIntent ->
        ViewState(
            audiobookFolders = folderNames.takeIf { it.isNotEmpty() }?.joinToEllipsizedString(),
            rateAppIntent = rateAppIntent,
            ttsEnabled = uiSettings.readBookTitles,
            uiMode = uiSettings.uiThemeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setUiMode(newUiMode: UiThemeMode) {
        mainScope.launchUpdate(uiSettingsStore) { it.copy(uiThemeMode = newUiMode) }
    }

    private fun playStorePageIntent() = Intent(Intent.ACTION_VIEW).apply {
        val packageName = appContext.packageName
        data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        // https://developer.android.com/distribute/marketing-tools/linking-to-google-play#android-app
        setPackage("com.android.vending")
    }
}