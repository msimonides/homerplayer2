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

import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineSessionManager
import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineSoapClient
import com.studio4plus.homerplayer2.daisyonline.DaisyOnlineSoapResult
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookFileEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookStatus
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineDao
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineServiceEntity
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.util.UUID

/**
 * Syncs the list of issued books for all configured DAISY Online services.
 *
 * For each service:
 * 1. Fetches the list of currently issued content IDs.
 * 2. Removes books that are no longer issued (and their audiobook entries).
 * 3. Inserts new books (status=PENDING).
 * 4. For each new book, fetches metadata and resource list, stores in DB.
 *
 * Returns true if all services synced successfully, false if any had errors.
 */
@Factory
class SyncDaisyOnlineBooks(
    private val daisyOnlineDao: DaisyOnlineDao,
    private val audiobooksDao: AudiobooksDao,
    soapClientFactoryLazy: Lazy<DaisyOnlineSoapClient>,
) {
    private val soapClientFactory by soapClientFactoryLazy

    suspend operator fun invoke(): Boolean {
        val services = daisyOnlineDao.getConfiguredServices()
        if (services.isEmpty()) {
            Timber.d("DaisyOnline: no configured services")
            return true
        }

        var allSuccess = true
        for (service in services) {
            val success = syncService(service)
            if (!success) allSuccess = false
        }
        return allSuccess
    }

    private suspend fun syncService(service: DaisyOnlineServiceEntity): Boolean {
        Timber.i("DaisyOnline: syncing service ${service.id}")
        val sessionManager = DaisyOnlineSessionManager(service, soapClientFactory)

        // Step 1: Fetch issued content IDs
        val issuedIds = when (val result = sessionManager.withSession { url ->
            getIssuedContentList(url)
        }) {
            is DaisyOnlineSoapResult.Success -> result.value
            is DaisyOnlineSoapResult.NoActiveSession -> {
                Timber.e("DaisyOnline: no active session for service ${service.id}")
                return false
            }
            is DaisyOnlineSoapResult.Error -> {
                Timber.e("DaisyOnline: error fetching content list for ${service.id}: ${result.message}")
                return false
            }
        }
        Timber.i("DaisyOnline: ${service.id} has ${issuedIds.size} issued books")

        // Step 2: Remove books no longer issued + their audiobook entries (in one transaction)
        // TODO: transaction?
        val removedAudiobookIds = daisyOnlineDao.syncIssuedBooks(service.id, issuedIds)
        removedAudiobookIds.forEach { audiobookId ->
            audiobooksDao.deleteAudiobook(audiobookId)
            Timber.i("DaisyOnline: removed returned book audiobookId=$audiobookId")
        }

        // Step 3: Find new books (not yet in DB)
        val existingIds = daisyOnlineDao.getContentIds(service.id).toSet()
        val newIds = issuedIds.filter { it !in existingIds }
        Timber.i("DaisyOnline: ${newIds.size} new books for service ${service.id}")

        // Step 4: Fetch metadata + resources for new books
        var success = true
        for (contentId in newIds) {
            val bookSuccess = fetchAndInsertBook(service, contentId, sessionManager)
            if (!bookSuccess) success = false
        }
        return success
    }

    private suspend fun fetchAndInsertBook(
        service: DaisyOnlineServiceEntity,
        contentId: String,
        sessionManager: DaisyOnlineSessionManager,
    ): Boolean {
        val metadata = when (val result = sessionManager.withSession { url ->
            getContentMetadata(url, contentId)
        }) {
            is DaisyOnlineSoapResult.Success -> result.value
            is DaisyOnlineSoapResult.NoActiveSession,
            is DaisyOnlineSoapResult.Error -> {
                val msg = if (result is DaisyOnlineSoapResult.Error) result.message else "no active session"
                Timber.e("DaisyOnline: failed to get metadata for $contentId: $msg")
                return false
            }
        }
        val resources = when (val result = sessionManager.withSession { url ->
            getContentResources(url, contentId)
        }) {
            is DaisyOnlineSoapResult.Success -> result.value
            is DaisyOnlineSoapResult.NoActiveSession,
            is DaisyOnlineSoapResult.Error -> {
                val msg = if (result is DaisyOnlineSoapResult.Error) result.message else "no active session"
                Timber.e("DaisyOnline: failed to get resources for $contentId: $msg")
                return false
            }
        }

        // Generate stable audiobook ID
        val audiobookId = UUID.nameUUIDFromBytes("${service.id}:$contentId".toByteArray()).toString()

        val book = DaisyOnlineBookEntity(
            serviceId = service.id,
            contentId = contentId,
            audiobookId = audiobookId,
            title = metadata.title,
            author = metadata.author,
            status = DaisyOnlineBookStatus.PENDING,
        )
        val files = resources.map { resource ->
            val fileId = UUID.nameUUIDFromBytes(resource.uri.toByteArray()).toString()
            DaisyOnlineBookFileEntity(
                remoteUri = resource.uri,
                serviceId = service.id,
                contentId = contentId,
                fileId = fileId,
                isDownloaded = false,
            )
        }
        daisyOnlineDao.insertBook(book, files)

        Timber.i("DaisyOnline: inserted book $contentId ('${metadata.title}') with ${files.size} files")
        return true
    }
}
