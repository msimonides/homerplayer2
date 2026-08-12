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

import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineFileStorage
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineDao
import org.koin.core.annotation.Factory
import timber.log.Timber

/**
 * Deletes tmp dirs for books that are no longer tracked in the database.
 *
 * This use case is called after sync/download passes to clean up orphaned tmp data
 * (e.g. books removed from the issued list while a download was in progress).
 */
@Factory
class DeleteStaleDaisyOnlineFiles(
    private val daisyOnlineDao: DaisyOnlineDao,
    private val fileStorage: DaisyOnlineFileStorage,
) {
    suspend operator fun invoke() {
        val services = daisyOnlineDao.getConfiguredServices()
        for (service in services) {
            val activeAudiobookIds = daisyOnlineDao.getAudiobookIdsForService(service.id)
            fileStorage.cleanupStaleTmpDirs(service.id, activeAudiobookIds.toSet())
            Timber.d("DaisyOnline: cleaned stale tmp dirs for service ${service.id}")
        }
    }
}
