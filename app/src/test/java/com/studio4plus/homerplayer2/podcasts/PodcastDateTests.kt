/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.podcasts

import com.studio4plus.homerplayer2.podcasts.usecases.ParsePodcastFeed.Companion.parseRssDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PodcastDateTests {

    @Test
    fun `parsing test`() {
        val regularDates = listOf(
            "Tue, 3 Jun 2008 11:05:30 GMT",
            "Tue, 3 Jun 2008 11:05:30 +0100",
            "3 Jun 2008 11:05:30 GMT",
            "Tue, 3 Jun 2008 1:05:30 GMT",
            "Tue, 3 Jun 2008 1:05 GMT",
        )
        val nonStandardDatesWithExpected = listOf(
            "Tue, 3 Jun 2008 11:05:30 UT" to "Tue, 3 Jun 2008 11:05:30 GMT",
            "Tue, 3 Jun 2008 11:05:30 EDT" to "Tue, 3 Jun 2008 11:05:30 -0400",
            "Tue, 3 Jun 2008 11:05:30 EST" to "Tue, 3 Jun 2008 11:05:30 -0500",
            "Tue, 3 Jun 2008 11:05:30 CDT" to "Tue, 3 Jun 2008 11:05:30 -0500",
            "Tue, 3 Jun 2008 11:05:30 CST" to "Tue, 3 Jun 2008 11:05:30 -0600",
            "Tue, 3 Jun 2008 11:05:30 MDT" to "Tue, 3 Jun 2008 11:05:30 -0600",
            "Tue, 3 Jun 2008 11:05:30 MST" to "Tue, 3 Jun 2008 11:05:30 -0700",
            "Tue, 3 Jun 2008 11:05:30 PDT" to "Tue, 3 Jun 2008 11:05:30 -0700",
            "Tue, 3 Jun 2008 11:05:30 PST" to "Tue, 3 Jun 2008 11:05:30 -0800",
        )

        fun String.parseExpected() =
            ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()

        (regularDates.map { it to it } + nonStandardDatesWithExpected)
            .forEach { (dateString, expectedDateString) ->
                assertEquals(
                    "Incorrectly parsed $dateString",
                    expectedDateString.parseExpected(),
                    dateString.parseRssDate()
                )
            }
    }

    @Test
    fun `parsing invalid date doesn't throw`() {
        val invalidDates = listOf(
            "invalid",
            "Tue, 3 Jun 2008 11:05:30",
            "Tue, 3 Jun 2008 11:05:30 ABC",
            "Tue, 3 Jun 2008 11:05:30 ",
        )
        invalidDates.forEach { assertNull(it.parseRssDate()) }
    }
}