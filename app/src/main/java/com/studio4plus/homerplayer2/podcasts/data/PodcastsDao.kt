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
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    val episodesUnordered: List<PodcastEpisode>
) {
    // @Relation doesn't provide ordering, sort explicitly. The number of episodes is less than ten.
    @Ignore
    val episodes = episodesUnordered.sortedBy { it.number }
}

@Dao
abstract class PodcastsDao {
    @Query("""SELECT * FROM podcasts ORDER BY title ASC""")
    @Transaction
    abstract fun observePodcasts(): Flow<List<PodcastWithEpisodes>>

    @Query("""SELECT * FROM podcasts ORDER BY title ASC""")
    abstract fun getPodcasts(): List<Podcast>

    @Query("""SELECT EXISTS (SELECT * FROM podcasts)""")
    abstract fun observeHasAnyPodcast(): Flow<Boolean>

    @Query("""SELECT * FROM podcasts WHERE feed_uri = :feedUri""")
    @Transaction
    abstract fun observePodcast(feedUri: String): Flow<PodcastWithEpisodes?>

    @Query("""SELECT * FROM podcasts WHERE feed_uri = :feedUri""")
    abstract fun getPodcast(feedUri: String): Podcast?

    @Query("""SELECT EXISTS (SELECT * FROM podcast_episodes WHERE file_id = :fileId)""")
    abstract fun hasEpisodeForFile(fileId: String): Boolean

    @Upsert
    abstract suspend fun upsert(podcast: Podcast)

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

    @Query("""UPDATE podcast_episodes SET is_downloaded = 1 WHERE uri = :episodeUri""")
    abstract suspend fun updateIsDownloaded(episodeUri: String)

    @Query("""SELECT * FROM podcast_episodes WHERE is_downloaded = 0""")
    abstract suspend fun getEpisodesForDownload(): List<PodcastEpisode>

    @Transaction
    open suspend fun updateEpisodes(newEpisodes: List<PodcastEpisode>): List<String> {
        val feedUri = newEpisodes.first().feedUri
        check(newEpisodes.all { feedUri == it.feedUri })
        val oldEpisodes = getPodcastEpisodes(feedUri)
        insertEpisodes(newEpisodes)
        deleteRemainingEpisodes(feedUri, newEpisodes.map { it.uri })
        return oldEpisodes.map { it.fileId } - newEpisodes.map { it.fileId }
    }

    @Transaction
    open suspend fun deletePodcast(feedUri: String): List<String> {
        val episodes = getPodcastEpisodes(feedUri)
        deletePodcastRow(feedUri)
        return episodes.map { it.fileId }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEpisodes(episodes: List<PodcastEpisode>)

    @Query("""DELETE FROM podcasts WHERE feed_uri = :feedUri""")
    protected abstract suspend fun deletePodcastRow(feedUri: String)

    @Query("""DELETE FROM podcast_episodes WHERE feed_uri = :feedUri AND uri NOT IN (:episodeUris)""")
    protected abstract suspend fun deleteRemainingEpisodes(feedUri: String, episodeUris: List<String>)

    @Query("""SELECT * FROM podcast_episodes WHERE feed_uri = :feedUri""")
    protected abstract suspend fun getPodcastEpisodes(feedUri: String): List<PodcastEpisode>
}