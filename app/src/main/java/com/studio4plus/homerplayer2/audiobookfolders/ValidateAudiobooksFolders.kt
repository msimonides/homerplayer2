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

package com.studio4plus.homerplayer2.audiobookfolders

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.utils.hasContentScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import timber.log.Timber

@Factory
class ValidateAudiobooksFolders(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val contentResolver: ContentResolver,
    private val audiobookFoldersDao: AudiobookFoldersDao,
    private val audiobookFolderManager: AudiobookFolderManager,
) {
    suspend operator fun invoke(): Boolean {
        val folderUris = audiobookFoldersDao.getAll().first()
            .map { it.uri }.filter { it.hasContentScheme() }
        return withContext(dispatcherProvider.Io) {
            val (_, inaccessible) = validatePermissions(folderUris)
            inaccessible.forEach { audiobookFolderManager.removeFolder(it) }
            inaccessible.isNotEmpty()
        }
    }

    private fun validatePermissions(folderUris: List<Uri>): Pair<List<Uri>, List<Uri>> {
        val appPermissions = contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
            .toSet()
        val (withPermission, withoutPermission) = folderUris.partition { appPermissions.contains(it) }
        val (accessible, notAccessible) = withPermission.partition { folderUri ->
            val document = DocumentFile.fromTreeUri(context, folderUri)
            document != null && document.exists() && document.isDirectory
        }
        logInvalidFolders(withoutPermission, notAccessible)
        return Pair(accessible, withoutPermission + notAccessible)
    }

    private fun logInvalidFolders(noPermission: Collection<Uri>, notAccessible: Collection<Uri>) {
        if (noPermission.isNotEmpty()) Timber.i("Folders with revoked permission: %s", noPermission.joinToString(", "))
        if (notAccessible.isNotEmpty()) Timber.i("Folders not accessible any more: %s", notAccessible.joinToString(", "))
    }
}