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

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobooks.ui.AudiobookFolderNamesFlow
import com.studio4plus.homerplayer2.audiobooks.ui.joinToEllipsizedString
import com.studio4plus.homerplayer2.logging.PrepareIntentForLogSharing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SettingsMainViewModel(
    private val mainScope: CoroutineScope,
    audiobookFolderNamesFlow: AudiobookFolderNamesFlow,
    private val prepareIntentForLogSharing: PrepareIntentForLogSharing,
) : ViewModel() {

    class ViewState(
        val audiobookFolders: String? = null,
    )

    val viewState = audiobookFolderNamesFlow.map { folderNames ->
        ViewState(
            folderNames.takeIf { it.isNotEmpty() }?.joinToEllipsizedString()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    suspend fun shareDiagnosticLogs(): Intent = prepareIntentForLogSharing()
}