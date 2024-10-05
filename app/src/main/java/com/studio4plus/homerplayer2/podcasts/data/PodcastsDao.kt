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

package com.studio4plus.homerplayer2.podcasts.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class PodcastWithEpisodes(
    @Embedded
    val podcast: Podcast,
    @Relation(
        entity = PodcastEpisode::class,
        parentColumn = "feed_uri",
        entityColumn = "feed_uri",
    )
    val episodes: List<PodcastEpisode>
)

@Dao
abstract class PodcastsDao {
    @Query("""SELECT * FROM podcasts ORDER BY title ASC""")
    abstract fun getPodcasts(): Flow<List<PodcastWithEpisodes>>

    @Upsert
    abstract suspend fun upsert(podcast: Podcast)

    @Query("""SELECT * FROM podcasts WHERE feed_uri = :feedUri""")
    abstract fun getPodcast(feedUri: String): Flow<PodcastWithEpisodes?>

    @Query("""SELECT * FROM podcast_episodes WHERE feed_uri = :feedUri""")
    abstract fun getEpisodes(feedUri: String): Flow<List<PodcastEpisode>>

    @Query("""UPDATE podcasts SET title = :newTitle WHERE feed_uri = :feedUri""")
    abstract suspend fun updatePodcast(feedUri: String, newTitle: String)

    @Query("""UPDATE podcasts SET title_override = :newTitleOverride WHERE feed_uri = :feedUri""")
    abstract suspend fun updatePodcastTitleOverride(feedUri: String, newTitleOverride: String?)

    @Query("""UPDATE podcasts SET download_episode_count = :newEpisodeCount WHERE feed_uri = :feedUri""")
    abstract suspend fun updatePodcastEpisodeCount(feedUri: String, newEpisodeCount: Int)

    @Query("""
        UPDATE podcasts
           SET include_podcast_title = :includePodcastTitle,
               include_episode_number = :includeEpisodeNumber,
               include_episode_title = :includeEpisodeTitle
         WHERE feed_uri = :feedUri
         """
    )
    abstract suspend fun updateEpisodeTitle(
        feedUri: String,
        includePodcastTitle: Boolean,
        includeEpisodeNumber: Boolean,
        includeEpisodeTitle: Boolean
    )

    @Transaction
    open suspend fun updateEpisodes(newEpisodes: List<PodcastEpisode>) {
        val feedUri = newEpisodes.first().feedUri
        check(newEpisodes.all { feedUri == it.feedUri})
        upsertEpisodes(newEpisodes)
        deleteRemainingEpisodes(feedUri, newEpisodes.map { it.uri })
    }

    @Upsert
    protected abstract suspend fun upsertEpisodes(episodes: List<PodcastEpisode>)

    @Query("""DELETE FROM podcast_episodes WHERE feed_uri = :feedUri AND uri NOT IN (:episodeUris)""")
    protected abstract suspend fun deleteRemainingEpisodes(feedUri: String, episodeUris: List<String>)
}