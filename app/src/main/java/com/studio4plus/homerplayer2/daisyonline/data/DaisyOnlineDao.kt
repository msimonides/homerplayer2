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

package com.studio4plus.homerplayer2.daisyonline.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class DaisyOnlineDao {

    // --- Services ---

    @Query("SELECT * FROM daisy_online_services WHERE is_configured = 1")
    abstract suspend fun getConfiguredServices(): List<DaisyOnlineServiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertService(service: DaisyOnlineServiceEntity)

    @Query("DELETE FROM daisy_online_services WHERE id = :serviceId")
    abstract suspend fun deleteService(serviceId: String)

    // --- Books ---

    @Query("SELECT content_id FROM daisy_online_books WHERE service_id = :serviceId")
    abstract suspend fun getContentIds(serviceId: String): List<String>

    @Query("SELECT * FROM daisy_online_books WHERE service_id = :serviceId AND status != 'DOWNLOADED'")
    abstract suspend fun getBooksNotDownloaded(serviceId: String): List<DaisyOnlineBookEntity>

    @Transaction
    open suspend fun insertBook(book: DaisyOnlineBookEntity, files: List<DaisyOnlineBookFileEntity>) {
        insertBook(book)
        insertBookFiles(files)
    }

    @Query(
        """UPDATE daisy_online_books
              SET status = :status
            WHERE service_id = :serviceId AND content_id = :contentId"""
    )
    abstract suspend fun updateBookStatus(
        serviceId: String,
        contentId: String,
        status: DaisyOnlineBookStatus,
    )

    @Query(
        """UPDATE daisy_online_books
              SET last_download_error = :error
            WHERE service_id = :serviceId AND content_id = :contentId"""
    )
    abstract suspend fun updateBookStatusWithError(
        serviceId: String,
        contentId: String,
        error: String?,
    )

    @Query(
        """UPDATE daisy_online_books
              SET status = 'DOWNLOADED', downloaded_at = :downloadedAt, last_download_error = NULL
            WHERE service_id = :serviceId AND content_id = :contentId"""
    )
    abstract suspend fun markBookDownloaded(
        serviceId: String,
        contentId: String,
        downloadedAt: Long,
    )

    @Query("DELETE FROM daisy_online_books WHERE service_id = :serviceId AND content_id NOT IN (:currentContentIds)")
    abstract suspend fun deleteBooksNotIn(serviceId: String, currentContentIds: List<String>)

    // --- Book files ---

    @Query("SELECT * FROM daisy_online_book_files WHERE service_id = :serviceId AND content_id = :contentId")
    abstract suspend fun getFilesForBook(serviceId: String, contentId: String): List<DaisyOnlineBookFileEntity>

    @Query("SELECT * FROM daisy_online_book_files WHERE service_id = :serviceId AND content_id = :contentId AND is_downloaded = 0")
    abstract suspend fun getPendingFilesForBook(
        serviceId: String,
        contentId: String,
    ): List<DaisyOnlineBookFileEntity>

    @Query("UPDATE daisy_online_book_files SET is_downloaded = 1 WHERE remote_uri = :remoteUri")
    abstract suspend fun markFileDownloaded(remoteUri: String)

    /**
     * Removes books that are no longer in the issued list for this service.
     * Returns the audiobookIds of removed books (for cleanup in the audiobooks table).
     */
    @Transaction
    open suspend fun syncIssuedBooks(
        serviceId: String,
        currentContentIds: List<String>,
    ): List<String> {
        return if (currentContentIds.isEmpty()) {
            val removed = getAudiobookIdsForService(serviceId)
            deleteAllBooksForService(serviceId)
            removed
        } else {
            val removed = getAudiobookIdsNotIn(serviceId, currentContentIds)
            deleteBooksNotIn(serviceId, currentContentIds)
            removed
        }
    }

    data class AudiobookId(
        val serviceId: String,
        val audiobookId: String,
    )

    @Query("SELECT audiobook_id FROM daisy_online_books WHERE service_id = :serviceId")
    abstract suspend fun getAudiobookIdsForService(serviceId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertBook(book: DaisyOnlineBookEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertBookFiles(files: List<DaisyOnlineBookFileEntity>)

    @Query("SELECT audiobook_id FROM daisy_online_books WHERE service_id = :serviceId AND content_id NOT IN (:currentContentIds)")
    protected abstract suspend fun getAudiobookIdsNotIn(serviceId: String, currentContentIds: List<String>): List<String>

    @Query("DELETE FROM daisy_online_books WHERE service_id = :serviceId")
    protected abstract suspend fun deleteAllBooksForService(serviceId: String)
}
