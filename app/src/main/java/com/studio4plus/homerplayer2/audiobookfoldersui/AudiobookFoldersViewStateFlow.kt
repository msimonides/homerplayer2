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

package com.studio4plus.homerplayer2.audiobookfoldersui

import android.net.Uri
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFoldersDao
import com.studio4plus.homerplayer2.audiobookfolders.AudiobooksFolder
import com.studio4plus.homerplayer2.audiobookfolders.AudiobooksUpdater
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallController
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import org.koin.core.annotation.Factory

@Factory
class AudiobookFoldersViewStateFlow(
    dispatcherProvider: DispatcherProvider,
    audiobookFoldersDao: AudiobookFoldersDao,
    samplesInstallController: SamplesInstallController,
    private val audiobooksFolderName: AudiobooksFolderName,
    audiobooksUpdater: AudiobooksUpdater,
) : Flow<List<AudiobookFolderViewState>> {
    private val folders = audiobookFoldersDao.getAll().map { folders ->
        folders.mapNotNull { folder ->
            audiobooksFolderName(folder)?.let { folderName ->
                AudiobookFolderViewState(
                    folderName,
                    folder.uri,
                    0,
                    "",
                    firstBookTitle = null,
                    isScanning = true,
                    samplesFolderState = SamplesFolderState.Installed.takeIf { folder.isSamplesFolder }
                )
            }
        }
    }.flowOn(dispatcherProvider.Io)

    private val isInstallingSamplesFlow = samplesInstallController.stateFlow
        .map { it != SamplesInstallState.Idle }
        .scan(false) { wasInstalling, isInstalling ->
            if (wasInstalling && !isInstalling)
                delay(500) // Give time for DAO to emit the newly added samples.
            isInstalling
        }

    private val flow = combine(
        folders,
        audiobookFoldersDao.getAllWithBookTitles(),
        audiobooksUpdater.isScanning,
        isInstallingSamplesFlow,
    ) { folderItems, foldersWithTitles, isScanning, isInstallingSamples ->
        val folderViews = folderItems.map { folder ->
            val bookTitles = foldersWithTitles[folder.uri]
            folder.copy(
                bookCount = bookTitles?.size ?: 0,
                bookTitles = bookTitles?.joinToEllipsizedString() ?: "",
                firstBookTitle = bookTitles?.firstOrNull(),
                isScanning = isScanning,
            )
        }
        if (isInstallingSamples && !folderViews.any { it.samplesFolderState != null  }) {
            folderViews + createSamplesPendingItem()
        } else {
            folderViews
        }
    }

    override suspend fun collect(collector: FlowCollector<List<AudiobookFolderViewState>>) {
        flow.collect(collector)
    }

    private fun createSamplesPendingItem() = AudiobookFolderViewState(
        audiobooksFolderName(AudiobooksFolder(Uri.EMPTY, isSamplesFolder = true)).orEmpty(),
        Uri.EMPTY,
        0,
        "",
        firstBookTitle = null,
        isScanning = false,
        samplesFolderState = SamplesFolderState.Downloading,
    )
}