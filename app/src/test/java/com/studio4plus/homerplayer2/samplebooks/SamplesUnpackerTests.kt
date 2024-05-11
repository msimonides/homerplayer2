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

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class) // Needed for JSON
@Config(application = Application::class)
class SamplesUnpackerTests {

    @get:Rule
    val rule = SampleBooksTestRule()

    @Test
    fun `when translation is missing then default is used`() = runTest {
        val installer = SamplesUnpacker(Locale("en", "us"))
        installer(rule.inputStream("book_samples.zip"), rule.outputFolder)

        val expected = setOf(
            "Sample Book 1",
            "Sample Book 2",
        )
        assertEquals(expected, rule.outputFolder.list()?.toSet())
    }

    @Test
    fun `when MX Spanish is missing then use generic Spanish`() = runTest {
        val locale = Locale("es", "mx")
        val installer = SamplesUnpacker(locale)
        installer(rule.inputStream("book_samples.zip"), rule.outputFolder)

        val expected = setOf("[es_MX] Sample Book 1", "[es] Sample Book 2")
        assertEquals(expected, rule.outputFolder.list()?.toSet())
    }
}