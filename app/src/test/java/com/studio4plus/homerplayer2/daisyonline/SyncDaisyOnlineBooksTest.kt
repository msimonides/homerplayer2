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

package com.studio4plus.homerplayer2.daisyonline

import android.app.Application
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookStatus
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineServiceEntity
import com.studio4plus.homerplayer2.daisyonline.usecases.SyncDaisyOnlineBooks
import com.studio4plus.homerplayer2.net.DownloadMeta
import com.studio4plus.homerplayer2.net.NetworkClient
import com.studio4plus.homerplayer2.net.NetworkResult
import com.studio4plus.homerplayer2.testutils.createInMemoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SyncDaisyOnlineBooksTest {

    private lateinit var db: AppDatabase

    private val service1 = DaisyOnlineServiceEntity(
        id = "service1",
        name = "Library 1",
        url = "https://lib1.example.com/daisy",
        username = "user1",
        password = "pass1",
    )

    @Before
    fun setUp() {
        db = createInMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createUseCase(client: FakeDaisyOnlineSoapClient): SyncDaisyOnlineBooks =
        SyncDaisyOnlineBooks(db.daisyOnlineDao(), db.audiobooksDao()) { client }

    @Test
    fun `sync with no services returns true`() = runBlocking {
        val result = createUseCase(FakeDaisyOnlineSoapClient()).invoke()
        assertTrue(result)
    }

    @Test
    fun `sync inserts new books with metadata and files`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(
            issuedIds = listOf("book1", "book2"),
            metadata = DaisyOnlineContentMetadata("Test Title", "Test Author"),
            resources = listOf(DaisyOnlineContentResource("https://cdn.example.com/file1.mp3", "audio/mpeg", 1024L)),
        )

        val result = createUseCase(client).invoke()

        assertTrue(result)
        val dao = db.daisyOnlineDao()
        val ids = dao.getContentIds(service1.id).toSet()
        assertEquals(setOf("book1", "book2"), ids)
        val files = dao.getFilesForBook(service1.id, "book1")
        assertEquals(1, files.size)
        assertEquals("https://cdn.example.com/file1.mp3", files[0].remoteUri)
        assertEquals(DaisyOnlineBookStatus.PENDING, dao.getBooksNotDownloaded(service1.id)[0].status)
    }

    @Test
    fun `sync removes books no longer in issued list`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(issuedIds = listOf("book1", "book2"))
        createUseCase(client).invoke()

        val client2 = FakeDaisyOnlineSoapClient(issuedIds = listOf("book1"))
        createUseCase(client2).invoke()

        val ids = db.daisyOnlineDao().getContentIds(service1.id).toSet()
        assertEquals(setOf("book1"), ids)
    }

    @Test
    fun `sync does not re-insert already known books`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(issuedIds = listOf("book1"))
        createUseCase(client).invoke()
        createUseCase(client).invoke()

        // Metadata was fetched only once (in the second sync book1 already exists)
        assertEquals("Metadata should be fetched only once", 1, client.metadataFetchCount)
    }

    @Test
    fun `sync returns false when content list fetch fails`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(contentListError = "Service unavailable")

        val result = createUseCase(client).invoke()

        assertFalse(result)
    }

    @Test
    fun `sync returns false when metadata fetch fails for one book but inserts others`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(
            issuedIds = listOf("book1", "book2"),
            metadataErrorForId = "book1",
            metadata = DaisyOnlineContentMetadata("Title", "Author"),
        )

        val result = createUseCase(client).invoke()

        assertFalse(result)
        val ids = db.daisyOnlineDao().getContentIds(service1.id).toSet()
        assertEquals("book2 should still be inserted", setOf("book2"), ids)
    }

    @Test
    fun `audiobookId is stable across syncs`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(issuedIds = listOf("book1"))
        createUseCase(client).invoke()
        val idAfterFirst = db.daisyOnlineDao().getBooksNotDownloaded(service1.id)[0].audiobookId

        createUseCase(client).invoke()
        val idAfterSecond = db.daisyOnlineDao().getBooksNotDownloaded(service1.id)[0].audiobookId

        assertEquals(idAfterFirst, idAfterSecond)
    }

    @Test
    fun `sync returns false on login failure`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        val client = FakeDaisyOnlineSoapClient(loginSuccess = false)

        val result = createUseCase(client).invoke()

        assertFalse(result)
    }

    @Test
    fun `sync removes audiobook when book is returned to library`() = runBlocking {
        db.daisyOnlineDao().insertService(service1)
        // First sync: insert book1 and book2
        val client = FakeDaisyOnlineSoapClient(issuedIds = listOf("book1", "book2"))
        createUseCase(client).invoke()

        // Manually mark book1 as downloaded and insert into audiobooks table (simulating completed download)
        val dao = db.daisyOnlineDao()
        val book1 = dao.getBooksNotDownloaded(service1.id).first { it.contentId == "book1" }
        dao.markBookDownloaded(service1.id, "book1", System.currentTimeMillis())
        db.audiobooksDao().insertAudiobook(
            com.studio4plus.homerplayer2.audiobooks.Audiobook(
                id = book1.audiobookId,
                displayName = "${book1.author}. ${book1.title}",
                primarySortKey = "${book1.author}. ${book1.title}",
                secondarySortKey = null,
                rootFolderUri = android.net.Uri.parse("daisyonline://${service1.id}"),
            ),
            emptyList()
        )

        // Second sync: book1 is no longer issued
        val client2 = FakeDaisyOnlineSoapClient(issuedIds = listOf("book2"))
        createUseCase(client2).invoke()

        // book1 should be removed from audiobooks
        val audiobooks = db.audiobooksDao().getAllForDebug()
        assertTrue(
            "book1 audiobook should be removed after it's returned",
            audiobooks.none { it.id == book1.audiobookId }
        )
        // book2 should still be in daisy_online_books
        assertEquals(setOf("book2"), dao.getContentIds(service1.id).toSet())
    }
}

/**
 * Test double for [DaisyOnlineSoapClient] that provides controllable responses.
 * Passes a no-op [NetworkClient] to the superclass constructor (never called in tests
 * since all relevant methods are overridden).
 */
private class FakeDaisyOnlineSoapClient(
    private val loginSuccess: Boolean = true,
    private val issuedIds: List<String> = emptyList(),
    private val contentListError: String? = null,
    private val metadata: DaisyOnlineContentMetadata = DaisyOnlineContentMetadata("Title", "Author"),
    private val metadataErrorForId: String? = null,
    private val resources: List<DaisyOnlineContentResource> = emptyList(),
) : DaisyOnlineSoapClient(NoopNetworkClient) {

    var metadataFetchCount = 0
    private var sessionActive = false

    override val hasSession: Boolean get() = sessionActive

    override suspend fun logOn(url: String, username: String, password: String): DaisyOnlineSoapResult<Boolean> {
        sessionActive = loginSuccess
        return DaisyOnlineSoapResult.Success(loginSuccess)
    }

    override fun clearSession() {
        sessionActive = false
    }

    override suspend fun getIssuedContentList(url: String): DaisyOnlineSoapResult<List<String>> =
        if (contentListError != null) DaisyOnlineSoapResult.Error(contentListError)
        else DaisyOnlineSoapResult.Success(issuedIds)

    override suspend fun getContentMetadata(url: String, contentId: String): DaisyOnlineSoapResult<DaisyOnlineContentMetadata> {
        metadataFetchCount++
        return if (metadataErrorForId == contentId) DaisyOnlineSoapResult.Error("Not found")
        else DaisyOnlineSoapResult.Success(metadata)
    }

    override suspend fun getContentResources(url: String, contentId: String): DaisyOnlineSoapResult<List<DaisyOnlineContentResource>> =
        DaisyOnlineSoapResult.Success(resources)
}

private object NoopNetworkClient : NetworkClient {
    override suspend fun getText(url: String, headers: Map<String, String>): NetworkResult<String> = notUsed()
    override suspend fun getBytes(url: String, headers: Map<String, String>): NetworkResult<ByteArray> = notUsed()
    override suspend fun head(url: String, headers: Map<String, String>): NetworkResult<Unit> = notUsed()
    override suspend fun postXml(url: String, body: String, headers: Map<String, String>): NetworkResult<String> = notUsed()
    override suspend fun downloadToFile(url: String, target: File, append: Boolean, rangeStart: Long?, headers: Map<String, String>): NetworkResult<DownloadMeta> = notUsed()
    private fun notUsed(): Nothing = error("NoopNetworkClient should not be called")
}
