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

package com.studio4plus.homerplayer2.samplebooks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class UnzipTests {

    @get:Rule
    val rule = SampleBooksTestRule()

    @Test
    fun `zip with 2 folders`() {
        unzip(rule.inputStream("zip_sample.zip"), rule.outputFolder)

        val book1Folder = File(rule.outputFolder, "book1")
        val book2Folder = File(rule.outputFolder, "book2")
        assertTrue(book1Folder.isDirectory)
        assertTrue(book2Folder.isDirectory)
        assertEquals(1L, File(book1Folder, "file1.txt").length())
        assertEquals(2L, File(book2Folder, "file1.txt").length())
        assertEquals(3L, File(book2Folder, "file2.txt").length())
    }

    // TODO: zip with path traversal

    @Test(expected = IllegalArgumentException::class)
    fun `when file is not ZIP then unzip fails`() {
        unzip(rule.inputStream("non_zip.txt"), rule.outputFolder)
    }
}