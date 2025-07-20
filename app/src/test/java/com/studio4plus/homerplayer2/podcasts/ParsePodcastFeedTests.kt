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

import android.app.Application
import com.prof18.rssparser.RssParser
import com.studio4plus.homerplayer2.podcasts.usecases.ParsePodcastFeed
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ParsePodcastFeedTests {

    private lateinit var parsePodcastFeed: ParsePodcastFeed

    @Before
    fun setup() {
        parsePodcastFeed = ParsePodcastFeed(RssParser())
    }

    @Test
    fun `take 5 latest episodes by date`() = runTest {
        val items = arrayOf(
            Item("Title 3", date = "3 Jun 2008 11:05:30 GMT"),
            Item("Title 2", date = "2 Jun 2008 11:05:30 GMT"),
            Item("Title 5", date = "5 Jun 2008 11:05:30 GMT"),
            Item("Title 1", date = "1 Jun 2008 11:05:30 GMT"),
            Item("Title 4", date = "4 Jun 2008 11:05:30 GMT"),
            Item("Title 6", date = "6 Jun 2008 11:05:30 GMT")
        )
        val rss = buildRssXml(*items)

        val podcastFeed = parsePodcastFeed(rss, "")
        assertNotNull(podcastFeed)
        assertEquals(
            listOf("Title 6", "Title 5", "Title 4", "Title 3", "Title 2"),
            podcastFeed.latestEpisodes.map { it.rssItem.title }
        )
    }

    @Test
    fun `take first 5 episodes when no dates`() = runTest {
        val items = (1 .. 6).reversed().map { Item("Title $it") }.toTypedArray()
        val rss = buildRssXml(*items)

        val podcastFeed = parsePodcastFeed(rss, "")
        assertNotNull(podcastFeed)
        assertEquals(
            listOf("Title 6", "Title 5", "Title 4", "Title 3", "Title 2"),
            podcastFeed.latestEpisodes.map { it.rssItem.title }
        )
    }

    @Test
    fun `episodes with date are newer than those without date`() = runTest {
        val items = arrayOf(
            Item("Title 3", date = "3 Jun 2008 11:05:30 GMT"),
            Item("Title 2", date = "2 Jun 2008 11:05:30 GMT"),
            Item("Title 1"),
            Item("Title 4", date = "4 Jun 2008 11:05:30 GMT"),
        )
        val rss = buildRssXml(*items)

        val podcastFeed = parsePodcastFeed(rss, "")
        assertNotNull(podcastFeed)
        assertEquals(
            listOf("Title 4", "Title 3", "Title 2", "Title 1"),
            podcastFeed.latestEpisodes.map { it.rssItem.title }
        )
    }

    private class Item(
        val title: String = "Title",
        val url: String = "https://dummy",
        val date: String? = null
    )

    private fun buildRssXml(vararg items: Item): String {
        val itemsXml = items.joinToString { item ->
            val pubDate = if (item.date != null) "<pubDate>${item.date}</pubDate>" else ""
            """
                <item>
                    <title>${item.title}</title>
                    <enclosure url="${item.url}" type="audio/mpeg" />
                    $pubDate
                </item>
            """.trimIndent()
        }
        return """
            <?xml version="1.0" encoding="UTF-8" ?>
            <rss version="2.0">
                <channel>
                    <title>Podcast</title>
            $itemsXml
                </channel>
            </rss>
        """.trimIndent()
    }
}