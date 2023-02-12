/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.onboarding

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobooks.AudiobookFolderManager
import com.studio4plus.homerplayer2.audiobooks.AudiobookFoldersDao
import com.studio4plus.homerplayer2.core.DispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

interface OnboardingFinishedObserver {
    fun onOnboardingFinished()
}

@KoinViewModel
class OnboardingAudiobookFoldersViewModel(
    private val appContext: Context,
    dispatcherProvider: DispatcherProvider,
    audiobookFoldersDao: AudiobookFoldersDao,
    private val audiobookFolderManager: AudiobookFolderManager,
    private val onboardingFinishedObserver: OnboardingFinishedObserver
) : ViewModel() {

    data class FolderItem(val displayName: String, val uri: Uri, val bookCount: Int?)

    data class ViewState(
        val folders: List<FolderItem>,
        val canProceed: Boolean
    )

    private val folders = audiobookFoldersDao.getAll().map { folders ->
        folders.mapNotNull { folder ->
            DocumentFile.fromTreeUri(appContext, folder.uri)?.let { documentFile ->
                FolderItem(documentFile.name ?: folder.toString(), folder.uri, null)
            }
        }
    }.flowOn(dispatcherProvider.Io)

    val viewState = combine(folders, audiobookFoldersDao.getAllWithBookCounts(), this::combineBookCounts)
        .map { ViewState(it, it.isNotEmpty()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ViewState(emptyList(), false)
        )

    fun addFolder(folderUri: Uri) = audiobookFolderManager.addFolder(folderUri)

    fun removeFolder(folder: FolderItem) = audiobookFolderManager.removeFolder(folder.uri)

    fun onFinished() {
        onboardingFinishedObserver.onOnboardingFinished()
    }

    private fun combineBookCounts(
        folderItems: List<FolderItem>,
        counts: Map<String, Int>
    ): List<FolderItem> =
        folderItems.map { folder ->
            folder.copy(bookCount = counts.getOrElse(folder.uri.toString()) { 0 })
        }
}