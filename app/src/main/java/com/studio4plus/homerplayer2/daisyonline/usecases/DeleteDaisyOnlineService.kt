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

import androidx.room.withTransaction
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineFileStorage
import org.koin.core.annotation.Factory
import timber.log.Timber

/**
 * Deletes a DAISY Online service and all associated data:
 * - Removes all audiobook entries from the audiobooks table.
 * - Deletes all downloaded files (tmp + final) from disk.
 * - Deletes the service record (CASCADE removes books and files from daisy_online_* tables).
 */
@Factory
class DeleteDaisyOnlineService(
    private val db: AppDatabase,
    private val audiobooksDao: AudiobooksDao,
    private val fileStorage: DaisyOnlineFileStorage,
) {
    suspend operator fun invoke(serviceId: String) {
        val dao = db.daisyOnlineDao()

        // Collect all audiobook IDs before deleting DB records
        val audiobookIds = dao.getAudiobookIdsForService(serviceId)

        // Remove audiobook entries and the service record in one transaction
        // (CASCADE in DB removes daisy_online_books and daisy_online_book_files automatically)
        db.withTransaction {
            audiobookIds.forEach { audiobooksDao.deleteAudiobook(it) }
            dao.deleteService(serviceId)
        }

        // Delete all files from disk (tmp + final) for each book
        audiobookIds.forEach { audiobookId ->
            fileStorage.deleteBookFiles(serviceId, audiobookId)
        }

        Timber.i("DaisyOnline: deleted service $serviceId with ${audiobookIds.size} books")
    }
}
