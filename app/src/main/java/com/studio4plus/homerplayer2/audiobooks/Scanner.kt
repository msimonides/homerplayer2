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
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import androidx.annotation.WorkerThread
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.MessageDigest

private val DOCUMENTS_PROJECTION = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_SIZE
)

private const val COLUMN_ID = 0
private const val COLUMN_DISPLAY_NAME = 1
private const val COLUMN_MIME_TYPE = 2
private const val COLUMN_SIZE = 3

private val SORT_BY_DISPLAY_NAME = DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC"

@Single
class Scanner(
    private val contentResolver: ContentResolver,
    private val dispatcherProvider: DispatcherProvider
) {
    data class ScanResult(val audiobook: Audiobook, val uris: List<Uri>)

    suspend fun scan(folderUris: List<Uri>): List<ScanResult> =
        withContext(dispatcherProvider.Io) {
            folderUris.flatMap { scanFolder(it) }
        }

    @WorkerThread
    private fun scanFolder(folderUri: Uri): List<ScanResult> {
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
                if (isFolder(cursor.getString(COLUMN_MIME_TYPE))) {
                        scanAudiobook(
                            folderUri,
                            cursor.getString(COLUMN_ID),
                            cursor.getString(COLUMN_DISPLAY_NAME)
                        )
                } else {
                    null
                }
            }
        }
    }

    private fun scanAudiobook(rootFolderUri: Uri, folderDocumentId: String, path: String): ScanResult {
        val files = scanAudiobookFiles(rootFolderUri, folderDocumentId, path)
        val sizeBuffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        val id = files.fold(MessageDigest.getInstance("MD5")) { md5, (_, path, size) ->
            md5.update(path.encodeToByteArray())
            sizeBuffer.putLong(0, size)
            md5.update(sizeBuffer)
            md5
        }.digest().let { digest ->
            Base64.encodeToString(digest, Base64.NO_PADDING or Base64.NO_WRAP)
        }
        return ScanResult(Audiobook(id, path, rootFolderUri), files.map { it.first })
    }

    private fun scanAudiobookFiles(
        rootUri: Uri, folderDocumentId: String, path: String
    ): List<Triple<Uri, String, Long>> {
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
                    val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId)
                    listOf(Triple(uri, filePath, cursor.getLong(COLUMN_SIZE)))
                }
            }
        }
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