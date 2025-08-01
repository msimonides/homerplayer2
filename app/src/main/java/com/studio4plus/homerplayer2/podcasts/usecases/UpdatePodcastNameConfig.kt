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

package com.studio4plus.homerplayer2.podcasts.usecases

import androidx.room.withTransaction
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.podcasts.data.PodcastWithEpisodes
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import org.koin.core.annotation.Factory

@Factory
class UpdatePodcastNameConfig(
    private val db: AppDatabase,
    private val podcastsDao: PodcastsDao,
    private val audiobooksDao: AudiobooksDao,
    private val podcastEpisodeName: PodcastEpisodeName,
) {
    suspend operator fun invoke(
        podcast: PodcastWithEpisodes,
        includePodcastTitle: Boolean,
        includeEpisodeDate: Boolean,
        includeEpisodeTitle: Boolean
    ) {
        db.withTransaction {
            podcastsDao.updateEpisodeTitle(
                podcast.podcast.feedUri,
                includePodcastTitle = includePodcastTitle,
                includeEpisodeDate = includeEpisodeDate,
                includeEpisodeTitle = includeEpisodeTitle
            )
            val updatedPodcast = requireNotNull(podcastsDao.getPodcast(podcast.podcast.feedUri))
            podcast.episodes.forEach { episode ->
                val newDisplayName = podcastEpisodeName(updatedPodcast, episode)
                audiobooksDao.updateAudiobookDisplayName(episode.fileId, newDisplayName)
            }
        }
    }
}