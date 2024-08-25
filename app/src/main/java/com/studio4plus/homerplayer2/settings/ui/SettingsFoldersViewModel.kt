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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobooks.AudiobookFolderManager
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersViewStateFlow
import com.studio4plus.homerplayer2.audiobookfoldersui.FolderItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SettingsFoldersViewModel(
    audiobookFoldersViewStateFlow: AudiobookFoldersViewStateFlow,
    private val audiobookFoldersManager: AudiobookFolderManager
): ViewModel() {

    data class ViewState(val folderItems: List<FolderItem>)

    val viewState = audiobookFoldersViewStateFlow
        .map { folders -> ViewState(folders) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState(emptyList()))

    fun addFolder(uri: Uri) = audiobookFoldersManager.addFolder(uri)

    fun removeFolder(item: FolderItem) = audiobookFoldersManager.removeFolder(item.uri)
}