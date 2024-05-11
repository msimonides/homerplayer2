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

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.studio4plus.homerplayer2.audiobooks.AudiobookFoldersDao
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory
class AudiobookFolderNamesFlow(
    dispatcherProvider: DispatcherProvider,
    audiobookFoldersDao: AudiobookFoldersDao,
    audiobooksFolderName: AudiobooksFolderName,
): Flow<List<String>> {

    private val foldersFlow =  audiobookFoldersDao.getAll().map { folders ->
        folders.mapNotNull { folder -> audiobooksFolderName(folder) }
    }.flowOn(dispatcherProvider.Io)

    override suspend fun collect(collector: FlowCollector<List<String>>) {
        foldersFlow.collect(collector)
    }
}