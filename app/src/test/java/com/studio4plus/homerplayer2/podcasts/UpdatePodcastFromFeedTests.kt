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
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import com.studio4plus.homerplayer2.podcasts.usecases.PodcastFeed
import com.studio4plus.homerplayer2.podcasts.usecases.PodcastFeedEpisode
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastEpisodes
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastFromFeed
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
class UpdatePodcastFromFeedTests {

    private lateinit var db: AppDatabase
    private lateinit var audiobooksDao: AudiobooksDao
    private lateinit var podcastsDao: PodcastsDao

    private val podcast = Podcast(
        feedUri = "dummyUri",
        title = "Podcast Title",
        titleOverride = null,
        includeEpisodeNumber = true,
        includePodcastTitle = true,
        includeEpisodeTitle = true,
        downloadEpisodeCount = 2,
    )

    private lateinit var updatePodcast: UpdatePodcastFromFeed

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        podcastsDao = db.podcastsDao()
        audiobooksDao = db.audiobooksDao()
        updatePodcast = UpdatePodcastFromFeed(
            UpdatePodcastEpisodes(db, audiobooksDao, podcastsDao)
        )

        runBlocking {
            podcastsDao.upsert(podcast)
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `insert 3 episodes in batches of 2`() = runTest {
        val items = makeRssItems(3)
        val feed = makeFeed(makeRssChannel(podcast.title, items.take(2)))
        updatePodcast(podcast, feed)

        val episodes = podcastsDao.getPodcastEpisodes(podcast.feedUri)
        assertEquals(setOf("Episode 1", "Episode 2"), episodes.map { it.title }.toSet())

        val updatedFeed = makeFeed(makeRssChannel(podcast.title, items))
        updatePodcast(podcast, updatedFeed)
        val episodesAfterUpdate = podcastsDao.getPodcastEpisodes(podcast.feedUri)
        assertEquals(setOf("Episode 2", "Episode 3"), episodesAfterUpdate.map { it.title }.toSet())
        assertEquals(listOf(2, 3), episodesAfterUpdate.map { it.number }.sorted())
    }

    private fun makeFeed(rssChannel: RssChannel) =
        PodcastFeed(
            rss = rssChannel,
            title = podcast.title,
            latestEpisodes = rssChannel.items.reversed().map {
                PodcastFeedEpisode(it, publicationTime = null)
            }
        )

    private fun makeRssChannel(title: String, items: List<RssItem>) =
        RssChannel(
            title = title,
            link = null,
            description = null,
            image = null,
            lastBuildDate = null,
            updatePeriod = null,
            items = items,
            itunesChannelData = null,
            youtubeChannelData = null,
        )

    private fun makeRssItems(itemCount: Int) =
        (1 .. itemCount).map { number -> makeRssItem(number) }

    private fun makeRssItem(number: Int) = RssItem(
        guid = null,
        title = "Episode $number",
        author = "Author",
        link = null,
        pubDate = null,
        description = null,
        content = null,
        image = null,
        audio = "dummy URL $number",
        video = null,
        sourceName = null,
        sourceUrl = null,
        categories = emptyList(),
        itunesItemData = null,
        commentsUrl = null,
        youtubeItemData = null,
        rawEnclosure = null,
    )
}