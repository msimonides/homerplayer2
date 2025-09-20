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
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.WorkerThread
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.utils.hasFileScheme
import io.sentry.Sentry
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import timber.log.Timber
import java.io.File

private val DOCUMENTS_PROJECTION = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_SIZE
)

private const val COLUMN_ID = 0
private const val COLUMN_DISPLAY_NAME = 1
private const val COLUMN_MIME_TYPE = 2

private const val SORT_BY_DISPLAY_NAME = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC"

private const val SENTRY_SCAN_INFO = "Scan info"

@Single
class Scanner(
    private val contentResolver: ContentResolver,
    private val dispatcherProvider: DispatcherProvider
) {
    data class ScanResult(val audiobook: Audiobook, val uris: List<Uri>)

    suspend fun scan(folderUris: List<Uri>): List<ScanResult> =
        withContext(dispatcherProvider.Io) {
            folderUris.flatMap {
                when {
                    it.hasFileScheme() -> scanFileFolder(it)
                    else -> scanDocumentFolder(it)
                }
            }.also {
                Sentry.removeExtra(SENTRY_SCAN_INFO)
            }
        }

    @WorkerThread
    private fun scanDocumentFolder(folderUri: Uri): List<ScanResult> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            DocumentsContract.getTreeDocumentId(folderUri)
        )
        Timber.d("Scan audiobooks in: %s", folderUri.toString())

        val cursor = contentResolver.query(childrenUri, DOCUMENTS_PROJECTION, null, null, null)
        return if (cursor == null) {
            Timber.w("null cursor returned for query %s", childrenUri.toString())
            emptyList()
        } else {
            cursor.mapNotNull {
                val fileName = cursor.getString(COLUMN_DISPLAY_NAME)
                if (fileName.startsWith(".")) return@mapNotNull null

                val bookId = "$folderUri/$fileName"
                val documentId = cursor.getString(COLUMN_ID)
                if (isFolder(cursor.getString(COLUMN_MIME_TYPE))) {
                    scanAudiobook(
                        bookId = bookId,
                        rootFolderUri = folderUri,
                        folderDocumentId = documentId,
                        folderName = fileName
                    )
                } else {
                    scanSingleFileAudiobook(
                        bookId = bookId,
                        rootFolderUri = folderUri,
                        documentId = documentId,
                        fileName = fileName,
                    )
                }
            }
        }
    }

    @WorkerThread
    private fun scanFileFolder(folderUri: Uri): List<ScanResult> {
        val folder = File(requireNotNull(folderUri.path))
        Timber.d("Scan audiobooks in: %s", folder.canonicalPath)
        val subfolders = requireNotNull(folder.listFiles { file: File -> file.isDirectory })
        return subfolders.map { bookFolder ->
            val bookId = Uri.fromFile(bookFolder).toString()
            scanAudiobook(bookId = bookId, folder = bookFolder, rootFolderUri = folderUri)
        }
    }

    private fun scanAudiobook(bookId: String, rootFolderUri: Uri, folderDocumentId: String, folderName: String): ScanResult {
        val files = scanAudiobookFiles(rootFolderUri, folderDocumentId, folderName)
        return scanAudiobook(bookId, rootFolderUri, folderName, files)
    }

    private fun scanAudiobook(bookId: String, rootFolderUri: Uri, folder: File): ScanResult {
        val files = scanAudiobookFiles(folder)
        return scanAudiobook(bookId, rootFolderUri, folder.name, files)
    }

    private fun scanAudiobook(bookId: String, rootFolderUri: Uri, displayName: String, fileUris: List<Uri>): ScanResult {
        return ScanResult(
            Audiobook(id = bookId, displayName = displayName, rootFolderUri = rootFolderUri),
            fileUris,
        )
    }

    private fun scanAudiobookFiles(
        rootUri: Uri, folderDocumentId: String, path: String
    ): List<Uri> {
        Sentry.setExtra(SENTRY_SCAN_INFO, "path '$path', rootUri '$rootUri'")
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocumentId)
        val cursor = contentResolver.query(
            childrenUri,
            DOCUMENTS_PROJECTION,
            null,
            null,
            SORT_BY_DISPLAY_NAME
        )
        return if (cursor == null) {
            Timber.w("null cursor returned for query %s", childrenUri.toString())
            emptyList()
        } else {
            cursor.flatMap {
                val filePath = path + "/" + cursor.getString(COLUMN_DISPLAY_NAME)
                val documentId = cursor.getString(COLUMN_ID)
                val mimeType = cursor.getString(COLUMN_MIME_TYPE)
                if (isFolder(mimeType)) {
                    scanAudiobookFiles(rootUri, documentId, filePath)
                } else {
                    listOf(DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId))
                }
            }
        }
    }

    private fun scanAudiobookFiles(folder: File): List<Uri> =
        folder.listFiles()?.flatMap { file ->
            when {
                file.isDirectory -> scanAudiobookFiles(file)
                // mp3 is good enough for samples. One day scanning should be improved to filter out
                // non-audio files for all scan types.
                file.isFile && file.extension == "mp3" -> listOf(Uri.fromFile(file))
                else -> emptyList()
            }
        } ?: emptyList()

    private fun scanSingleFileAudiobook(
        bookId: String,
        rootFolderUri: Uri,
        documentId: String,
        fileName: String
    ): ScanResult {
        val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootFolderUri, documentId)
        val displayName = fileName.substringBeforeLast(".")
        return scanAudiobook(bookId, rootFolderUri, displayName, listOf(fileUri))
    }

    private fun isFolder(mimeType: String?): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR == mimeType
    }

    private inline fun <V> Cursor.mapNotNull(transform: (Cursor) -> V?): List<V> = use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val result = transform(cursor)
                if (result != null) add(result)
            }
        }
    }

    private inline fun <T, V : List<T>> Cursor.flatMap(transform: (Cursor) -> V?) = mapNotNull(transform).flatten()
}