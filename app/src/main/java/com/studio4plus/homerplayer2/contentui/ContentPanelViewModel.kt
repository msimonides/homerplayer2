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

package com.studio4plus.homerplayer2.contentui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFolderManager
import com.studio4plus.homerplayer2.audiobookfoldersui.FolderItem
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallController
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class ContentPanelViewModel(
    private val mainScope: CoroutineScope,
    private val audiobookFolderManager: AudiobookFolderManager,
    private val samplesInstaller: SamplesInstallController,
) : ViewModel() {

    sealed interface ErrorEvent {
        data class SamplesInstallError(
            val error: com.studio4plus.homerplayer2.samplebooks.SamplesInstallError
        ) : ErrorEvent
        object AddFolderExistsError : ErrorEvent
    }

    private val clearErrorEvent = MutableSharedFlow<ErrorEvent?>(extraBufferCapacity = 1)
    private val eventAddFolderError = Channel<ErrorEvent.AddFolderExistsError>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errorEvent : Flow<ErrorEvent?> = merge(
        clearErrorEvent,
        samplesInstaller.errorEvent.map { ErrorEvent.SamplesInstallError(it) },
        eventAddFolderError.receiveAsFlow()
    )

    fun addFolder(folderUri: Uri) = mainScope.launch {
        val success = audiobookFolderManager.addFolder(folderUri)
        if (!success)
            eventAddFolderError.trySend(ErrorEvent.AddFolderExistsError)
    }

    fun removeFolder(folder: FolderItem) {
        clearErrorSnack()
        audiobookFolderManager.removeFolder(folder.uri)
    }

    fun startSamplesInstall() {
        clearErrorSnack()
        samplesInstaller.start()
    }

    fun clearErrorSnack() {
        clearErrorEvent.tryEmit(null)
    }

    companion object {
        fun errorEventMessage(context: Context, event: ErrorEvent): String = when (event) {
            is ErrorEvent.AddFolderExistsError ->
                context.getString(R.string.content_add_error_folder_exists)

            is ErrorEvent.SamplesInstallError ->
                when (event.error) {
                    is SamplesInstallError.Download ->
                        context.getString(R.string.content_samples_download_error)

                    is SamplesInstallError.Install ->
                        context.getString(
                            R.string.content_samples_install_error,
                            event.error.error
                        )
                }
        }
    }
}