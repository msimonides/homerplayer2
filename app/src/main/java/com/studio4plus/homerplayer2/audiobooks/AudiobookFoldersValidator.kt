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
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber

class AudiobookFoldersValidator(
    private val context: Context,
    private val contentResolver: ContentResolver
) {

    // TODO: finish implementation

    @WorkerThread
    private fun validatePermissions(folderUris: List<FolderUri>): Pair<List<FolderUri>, List<FolderUri>> {
        val appPermissions = contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet()
        val (withPermission, withoutPermission) = folderUris.partition { appPermissions.contains(it.uriString) }
        val (accessible, notAccessible) = withPermission.partition { folderUri ->
            val document = DocumentFile.fromTreeUri(context, folderUri.toUri())
            document != null && document.exists() && document.isDirectory
        }
        logRemovedFolders(withoutPermission, notAccessible)
        return Pair(accessible, withoutPermission + notAccessible)
    }

    private fun logRemovedFolders(noPermission: Collection<FolderUri>, notAccessible: Collection<FolderUri>) {
        if (noPermission.isNotEmpty()) Timber.i("Folders with revoked permission: %s", noPermission.joinToString(", "))
        if (notAccessible.isNotEmpty()) Timber.i("Folders not accessible any more: %s", notAccessible.joinToString(", "))
    }
}