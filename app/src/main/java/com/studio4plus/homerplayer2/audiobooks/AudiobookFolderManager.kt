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

package com.studio4plus.homerplayer2.audiobooks

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.studio4plus.homerplayer2.utils.hasContentScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import timber.log.Timber

@Single
class AudiobookFolderManager(
    private val mainScope: CoroutineScope,
    private val contentResolver: ContentResolver,
    private val dao: AudiobookFoldersDao
) {

    fun addFolder(folder: Uri) {
        Timber.i("Adding audiobooks folder: %s", folder.toString())
        contentResolver.takePersistableUriPermission(folder, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        mainScope.launch {
            dao.insert(AudiobooksFolder(folder, isSamplesFolder = false))
            dao.deleteSamplesFolder()
        }
    }

    fun addSamplesFolder(folder: Uri) {
        Timber.i("Adding samples folder: %s", folder.toString())
        mainScope.launch {
            dao.insert(AudiobooksFolder(folder, isSamplesFolder = true))
        }
    }

    fun removeFolder(folder: Uri) {
        Timber.i("Removing audiobooks folder: %s", folder.toString())
        if (folder.hasContentScheme()) {
            try {
                contentResolver.releasePersistableUriPermission(
                    folder,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Timber.w(e, "Error releasing permission for: %s", folder.toString())
            }
        }
        mainScope.launch {
            dao.delete(folder)
        }
    }
}