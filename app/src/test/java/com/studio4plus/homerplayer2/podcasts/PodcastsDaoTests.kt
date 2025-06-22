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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class PodcastsDaoTests {

    private lateinit var podcastsDao: PodcastsDao
    private lateinit var db: AppDatabase

    private val podcastUri1 = "podcast_1"
    private val podcastUri2 = "podcast_2" // To test isolation

    private val episodeP1E1 = podcastEpisode(uri = "p1e1", feedUri = podcastUri1)
    private val episodeP1E2 = podcastEpisode(uri = "p1e2", feedUri = podcastUri1)
    private val episodeP2E1 = podcastEpisode(uri = "p2e1", feedUri = podcastUri2)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        podcastsDao = db.podcastsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `updateEpisodes with empty list clears episodes for target podcast only`() = runBlocking {
        podcastsDao.upsert(podcast(podcastUri1))
        podcastsDao.upsert(podcast(podcastUri2))
        podcastsDao.updateEpisodes(podcastUri1, listOf(episodeP1E1, episodeP1E2))
        podcastsDao.updateEpisodes(podcastUri2, listOf(episodeP2E1))

        podcastsDao.updateEpisodes(podcastUri1, emptyList())

        val episodesForP1AfterUpdate = podcastsDao.getPodcastEpisodes(podcastUri1)
        assertTrue(
            "Episodes for podcast '$podcastUri1' should be empty after updating with an empty list.",
            episodesForP1AfterUpdate.isEmpty()
        )

        val episodesForP2AfterUpdate = podcastsDao.getPodcastEpisodes(podcastUri2)
        assertEquals(
            "Number of episodes for podcast '$podcastUri2' should remain unchanged.",
            1,
            episodesForP2AfterUpdate.size
        )
        assertTrue(
            "Episodes for podcast '$podcastUri2' should still contain its original episode.",
            episodesForP2AfterUpdate.contains(episodeP2E1)
        )
    }

    private fun podcastEpisode(
        uri: String,
        number: Int = 1,
        title: String = "title",
        publicationTime: Instant? = Instant.ofEpochSecond(1000),
        feedUri: String = "https://feedUri",
        isDownloaded: Boolean = true,
        fileId: String = "fileId"
    ) = PodcastEpisode(uri, number, title, publicationTime, feedUri, isDownloaded, fileId)

    private fun podcast(
        feedUri: String,
        title: String = "podcast title",
        titleOverride: String? = null,
        includeEpisodeNumber: Boolean = true,
        includePodcastTitle: Boolean = true,
        includeEpisodeTitle: Boolean = true,
        downloadEpisodeCount: Int = 2,
    ) = Podcast(
        feedUri = feedUri,
        title = title,
        titleOverride = titleOverride,
        includeEpisodeNumber = includeEpisodeNumber,
        includePodcastTitle = includePodcastTitle,
        includeEpisodeTitle = includeEpisodeTitle,
        downloadEpisodeCount = downloadEpisodeCount
    )
}