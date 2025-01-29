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

package com.studio4plus.homerplayer2.audiobookfoldersui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFoldersDao
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.audiobooks.currentPositionMs
import com.studio4plus.homerplayer2.audiobooks.totalDurationMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AudiobooksFolderEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val mainScope: CoroutineScope,
    private val audiobooksFolderName: AudiobooksFolderName,
    private val audiobooksFoldersDao: AudiobookFoldersDao,
    audiobooksDao: AudiobooksDao,
) : ViewModel() {

    data class AudiobookViewState(
        val id: String,
        val displayName: String,
        val currentPositionMs: Long?,
        val totalDurationMs: Long?,
    )

    data class ViewState(
        val folderName: String,
        val rewindOnEnd: Boolean,
        val audiobooks: List<AudiobookViewState>
    )

    private val folderUri = Uri.parse(savedStateHandle[AudiobooksFolderEditNav.FolderUriKey] ?: "")

    val viewState: StateFlow<ViewState?> =
        combine(
            audiobooksFoldersDao.getFolderWithSettings(folderUri),
            audiobooksDao.getAllInFolder(folderUri)
        ) { folderWithSettings, audiobooks ->
            ViewState(
                folderName = audiobooksFolderName(folderWithSettings.folder) ?: "",
                rewindOnEnd = folderWithSettings.settings.rewindOnEnd,
                audiobooks = audiobooks.map {
                    val totalDurationMs = it.totalDurationMs()
                    AudiobookViewState(
                        id = it.audiobook.id,
                        displayName = it.audiobook.displayName,
                        currentPositionMs = if (totalDurationMs != null) it.currentPositionMs() else null,
                        totalDurationMs = totalDurationMs,
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun changeOnRewindOnEnd(enabled: Boolean) {
        mainScope.launch {
            audiobooksFoldersDao.updateFolderSettings(folderUri) { it.copy(rewindOnEnd = enabled) }
        }
    }
}