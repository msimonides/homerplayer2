/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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

package com.studio4plus.homerplayer2.daisyonline.usecases

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineFileStorage
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookFileEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookStatus
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineDao
import com.studio4plus.homerplayer2.net.FileDownloader
import io.ktor.utils.io.CancellationException
import org.koin.core.annotation.Single
import timber.log.Timber
import java.io.IOException

/**
 * Downloads all pending files for all DAISY Online books.
 *
 * For each service, for each non-downloaded book:
 * 1. Sets book status to DOWNLOADING.
 * 2. Downloads each pending file to tmp dir (resumable).
 * 3. If all files for a book are downloaded, moves tmp → final, registers in audiobooks table,
 *    and sets status to DOWNLOADED — all in a single transaction.
 * 4. On file error, records the error and continues with other files/books.
 *
 * Returns true if no errors occurred.
 */
@Single
class DownloadPendingDaisyOnlineBooks(
    private val db: AppDatabase,
    private val daisyOnlineDao: DaisyOnlineDao,
    private val audiobooksDao: AudiobooksDao,
    private val fileStorage: DaisyOnlineFileStorage,
    private val fileDownloader: FileDownloader,
) {
    suspend operator fun invoke(): Boolean {
        val services = daisyOnlineDao.getConfiguredServices()
        var allSuccess = true

        for (service in services) {
            val books = daisyOnlineDao.getBooksNotDownloaded(service.id)
            for (book in books) {
                val success = downloadBook(book)
                if (!success) allSuccess = false
            }
            // Clean up stale tmp dirs for this service (audiobook IDs no longer in DB)
            val activeAudiobookIds = daisyOnlineDao.getAudiobookIdsForService(service.id)
            fileStorage.cleanupStaleTmpDirs(service.id, activeAudiobookIds.toSet())
        }

        return allSuccess
    }

    private suspend fun downloadBook(book: DaisyOnlineBookEntity): Boolean {
        val serviceId = book.serviceId
        val contentId = book.contentId
        val audiobookId = book.audiobookId

        daisyOnlineDao.updateBookStatus(serviceId, contentId, DaisyOnlineBookStatus.DOWNLOADING)
        fileStorage.ensureTmpBookDir(serviceId, audiobookId)

        val pendingFiles = daisyOnlineDao.getPendingFilesForBook(serviceId, contentId)
        var bookHadError = false

        suspend fun handleError(remoteUri: String, e: Throwable) {
            bookHadError = true
            val errorMsg = e.message ?: e.javaClass.simpleName
            Timber.e(e, "DaisyOnline: error downloading $remoteUri")
            daisyOnlineDao.updateBookStatusWithError(
                serviceId = serviceId,
                contentId = contentId,
                error = errorMsg,
            )
        }

        for (fileEntity in pendingFiles) {
            val tmpFile = fileStorage.getBookFile(serviceId, audiobookId, fileEntity.fileId, inProgress = true)
            try {
                fileDownloader(tmpFile, fileEntity.remoteUri, append = true)
                daisyOnlineDao.markFileDownloaded(fileEntity.remoteUri)
                Timber.i("DaisyOnline: downloaded file ${fileEntity.fileId} for book $contentId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                handleError(fileEntity.remoteUri, e)
            } catch (e: Throwable) {
                handleError(fileEntity.remoteUri, e)
            }
        }

        // Check whether all files for this book are now downloaded.
        val remainingFiles = daisyOnlineDao.getPendingFilesForBook(serviceId, contentId)
        return if (remainingFiles.isEmpty()) {
            completeBookDownload(book)
            true
        } else {
            if (!bookHadError) {
                // No pending files and no error means all were already downloaded — fix status.
                completeBookDownload(book)
            } else {
                // Revert status to PENDING so next run retries.
                daisyOnlineDao.updateBookStatus(serviceId, contentId, DaisyOnlineBookStatus.PENDING)
            }
            !bookHadError
        }
    }

    private suspend fun completeBookDownload(book: DaisyOnlineBookEntity) {
        val serviceId = book.serviceId
        val contentId = book.contentId
        val audiobookId = book.audiobookId

        // Move files from tmp to final dir before registering in audiobooks
        fileStorage.moveTmpToFinal(serviceId, audiobookId)

        val allFiles = daisyOnlineDao.getFilesForBook(serviceId, contentId)
        db.withTransaction {
            addAudiobook(book, allFiles)
            daisyOnlineDao.markBookDownloaded(
                serviceId = serviceId,
                contentId = contentId,
                downloadedAt = System.currentTimeMillis(),
            )
        }
        Timber.i("DaisyOnline: book $contentId ('${book.title}') fully downloaded and added to library")
    }

    private suspend fun addAudiobook(book: DaisyOnlineBookEntity, files: List<DaisyOnlineBookFileEntity>) {
        val serviceId = book.serviceId
        val audiobookId = book.audiobookId
        val displayName = "${book.author}. ${book.title}"

        val audiobook = Audiobook(
            id = audiobookId,
            displayName = displayName,
            primarySortKey = displayName,
            secondarySortKey = null,
            rootFolderUri = daisyOnlineUri(serviceId),
        )

        val audiobookFiles = files.mapIndexed { index, fileEntity ->
            val finalFile = fileStorage.getBookFile(serviceId, audiobookId, fileEntity.fileId, inProgress = false)
            AudiobookFile(
                uri = Uri.fromFile(finalFile),
                position = index,
                bookId = audiobookId,
            )
        }

        audiobooksDao.insertAudiobook(audiobook, audiobookFiles)
        Timber.i("DaisyOnline: added audiobook '${audiobook.displayName}' with ${audiobookFiles.size} files")
    }
}

private fun daisyOnlineUri(serviceId: String): Uri = "daisyonline://$serviceId".toUri()
