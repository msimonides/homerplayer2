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
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookFileEntity
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineBookStatus
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineDao
import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineServiceEntity
import com.studio4plus.homerplayer2.testutils.createInMemoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DaisyOnlineDaoTests {

    private lateinit var db: AppDatabase
    private lateinit var dao: DaisyOnlineDao

    private val service1 = DaisyOnlineServiceEntity(
        id = "service1",
        name = "Library 1",
        url = "https://lib1.example.com/daisy",
        username = "user1",
        password = "pass1",
    )
    private val service2 = DaisyOnlineServiceEntity(
        id = "service2",
        name = "Library 2",
        url = "https://lib2.example.com/daisy",
        username = "user2",
        password = "pass2",
    )

    @Before
    fun createDb() {
        db = createInMemoryDatabase()
        dao = db.daisyOnlineDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `getConfiguredServices returns only configured services`() = runBlocking {
        dao.insertService(service1)
        dao.insertService(service2.copy(isConfigured = false))

        val services = dao.getConfiguredServices()

        assertEquals(1, services.size)
        assertEquals(service1.id, services[0].id)
    }

    @Test
    fun `insertBook and getContentIds`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id), emptyList())
        dao.insertBook(book("book2", service1.id), emptyList())

        val ids = dao.getContentIds(service1.id)

        assertEquals(setOf("book1", "book2"), ids.toSet())
    }

    @Test
    fun `insertBook does not overwrite existing book (IGNORE strategy)`() = runBlocking {
        dao.insertService(service1)
        val original = book("book1", service1.id, title = "Original Title")
        val duplicate = book("book1", service1.id, title = "New Title")
        // TODO: test with some files.
        dao.insertBook(original, emptyList())
        dao.insertBook(duplicate, emptyList())

        val booksNotDownloaded = dao.getBooksNotDownloaded(service1.id)

        assertEquals(1, booksNotDownloaded.size)
        assertEquals("Original Title", booksNotDownloaded[0].title)
    }

    @Test
    fun `syncIssuedBooks removes books not in current list`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id), emptyList())
        dao.insertBook(book("book2", service1.id), emptyList())
        dao.insertBook(book("book3", service1.id), emptyList())

        dao.syncIssuedBooks(service1.id, listOf("book1", "book3"))

        val ids = dao.getContentIds(service1.id).toSet()
        assertEquals(setOf("book1", "book3"), ids)
    }

    @Test
    fun `syncIssuedBooks returns audiobookIds of removed books`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id, audiobookId = "ab-book1"), emptyList())
        dao.insertBook(book("book2", service1.id, audiobookId = "ab-book2"), emptyList())
        dao.insertBook(book("book3", service1.id, audiobookId = "ab-book3"), emptyList())

        val removed = dao.syncIssuedBooks(service1.id, listOf("book1", "book3"))

        assertEquals(listOf("ab-book2"), removed)
    }

    @Test
    fun `syncIssuedBooks returns all audiobookIds when list is empty`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id, audiobookId = "ab-book1"), emptyList())
        dao.insertBook(book("book2", service1.id, audiobookId = "ab-book2"), emptyList())

        val removed = dao.syncIssuedBooks(service1.id, emptyList())

        assertEquals(setOf("ab-book1", "ab-book2"), removed.toSet())
    }

    @Test
    fun `syncIssuedBooks with empty list removes all books for service`() = runBlocking {
        dao.insertService(service1)
        dao.insertService(service2)
        dao.insertBook(book("book1", service1.id), emptyList())
        dao.insertBook(book("book2", service2.id), emptyList())

        dao.syncIssuedBooks(service1.id, emptyList())

        assertTrue(dao.getContentIds(service1.id).isEmpty())
        assertEquals(1, dao.getContentIds(service2.id).size)
    }

    @Test
    fun `syncIssuedBooks only removes books for target service`() = runBlocking {
        dao.insertService(service1)
        dao.insertService(service2)
        dao.insertBook(book("sharedId", service1.id), emptyList())
        dao.insertBook(book("sharedId", service2.id), emptyList())

        dao.syncIssuedBooks(service1.id, emptyList())

        assertTrue(dao.getContentIds(service1.id).isEmpty())
        assertEquals(listOf("sharedId"), dao.getContentIds(service2.id))
    }

    @Test
    fun `deleteService cascades to books and files`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(
            book("book1", service1.id),
            listOf(bookFile("uri1", "service1", "book1"))
        )

        dao.deleteService(service1.id)

        assertTrue(dao.getContentIds(service1.id).isEmpty())
        assertTrue(dao.getFilesForBook("service1", "book1").isEmpty())
    }

    @Test
    fun `updateBookStatus changes status`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id), emptyList())

        dao.updateBookStatus(service1.id, "book1", DaisyOnlineBookStatus.DOWNLOADING)

        val books = dao.getBooksNotDownloaded(service1.id)
        assertEquals(DaisyOnlineBookStatus.DOWNLOADING, books[0].status)
    }

    @Test
    fun `markBookDownloaded sets status and timestamp`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(book("book1", service1.id), emptyList())

        dao.markBookDownloaded(service1.id, "book1", downloadedAt = 1000L)

        val books = dao.getBooksNotDownloaded(service1.id)
        assertTrue("Downloaded book should not appear in not-downloaded list", books.isEmpty())
    }

    @Test
    fun `getPendingFilesForBook returns only not-downloaded files`() = runBlocking {
        dao.insertService(service1)
        val files = listOf(
            bookFile("uri1", service1.id, "book1"),
            bookFile("uri2", service1.id, "book1"),
        )
        dao.insertBook(
            book("book1", service1.id),
            files
        )

        dao.markFileDownloaded("uri1")

        val pending = dao.getPendingFilesForBook(service1.id, "book1")

        assertEquals(1, pending.size)
        assertEquals("uri2", pending[0].remoteUri)
    }

    @Test
    fun `upsertBookFiles ignores duplicate remote URIs`() = runBlocking {
        dao.insertService(service1)
        val file = bookFile("uri1", service1.id, "book1")
        val identicalFiles = listOf(file, file)
        dao.insertBook(book("book1", service1.id), identicalFiles)

        val files = dao.getFilesForBook(service1.id, "book1")
        assertEquals(1, files.size)
    }

    @Test
    fun `files cascade-delete when book is removed`() = runBlocking {
        dao.insertService(service1)
        dao.insertBook(
            book("book1", service1.id),
            listOf(bookFile("uri1", service1.id, "book1"))
        )

        dao.syncIssuedBooks(service1.id, emptyList())

        assertTrue(dao.getFilesForBook(service1.id, "book1").isEmpty())
    }

    @Test
    fun `multi-service books are isolated`() = runBlocking {
        dao.insertService(service1)
        dao.insertService(service2)
        dao.insertBook(book("book1", service1.id, title = "Service1 Book"), emptyList())
        dao.insertBook(book("book1", service2.id, title = "Service2 Book"), emptyList())

        val s1Books = dao.getBooksNotDownloaded(service1.id)
        val s2Books = dao.getBooksNotDownloaded(service2.id)

        assertEquals(1, s1Books.size)
        assertEquals("Service1 Book", s1Books[0].title)
        assertEquals(1, s2Books.size)
        assertEquals("Service2 Book", s2Books[0].title)
    }

    // --- Helpers ---

    private fun book(
        contentId: String,
        serviceId: String,
        title: String = "Test Book",
        author: String = "Test Author",
        status: DaisyOnlineBookStatus = DaisyOnlineBookStatus.PENDING,
        audiobookId: String = "audiobook-$serviceId-$contentId",
    ) = DaisyOnlineBookEntity(
        serviceId = serviceId,
        contentId = contentId,
        audiobookId = audiobookId,
        title = title,
        author = author,
        status = status,
    )

    private fun bookFile(
        remoteUri: String,
        serviceId: String,
        contentId: String,
        fileId: String = "file-${remoteUri.hashCode()}",
        isDownloaded: Boolean = false,
    ) = DaisyOnlineBookFileEntity(
        remoteUri = remoteUri,
        serviceId = serviceId,
        contentId = contentId,
        fileId = fileId,
        isDownloaded = isDownloaded,
    )
}
