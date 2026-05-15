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

package com.studio4plus.homerplayer2.audiobooks

import android.app.Application
import android.net.Uri
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.testutils.createInMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AudiobooksDaoTests {

    private lateinit var audiobooksDao: AudiobooksDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = createInMemoryDatabase()
        audiobooksDao = db.audiobooksDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `getAudiobook returns files in insertion order`() = runBlocking {
        val audiobook = Audiobook(
            id = "book-1",
            displayName = "Book 1",
            primarySortKey = "Book 1",
            secondarySortKey = null,
            rootFolderUri = Uri.parse("content://library/book-1")
        )
        val file1 = AudiobookFile(
            uri = Uri.parse("content://library/doc/2"),
            position = 1,
            bookId = audiobook.id,
        )
        val file2 = AudiobookFile(
            uri = Uri.parse("content://library/doc/1"),
            position = 2,
            bookId = audiobook.id,
        )

        audiobooksDao.insertAudiobook(audiobook, listOf(file1, file2))
        audiobooksDao.insertAudiobookFileDurations(
            listOf(
                AudiobookFileDuration(uri = file1.uri, durationMs = 1000),
                AudiobookFileDuration(uri = file2.uri, durationMs = 1000),
            )
        )

        val files = audiobooksDao.getAudiobook(audiobook.id).first()?.files.orEmpty()

        assertEquals(listOf(file1.uri, file2.uri), files.map { it.uri })
    }
}

