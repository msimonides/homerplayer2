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

import com.studio4plus.homerplayer2.net.toHttps
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import org.koin.core.annotation.Factory
import java.util.UUID

@Factory
class UpdatePodcastFromFeed(
    private val updatePodcastEpisodes: UpdatePodcastEpisodes,
) {
    // TODO: unit tests
    suspend operator fun invoke(podcast: Podcast, feed: PodcastFeed): Boolean {
        val latestEpisodes = feed.latestEpisodes.take(podcast.downloadEpisodeCount)
        val totalCount = feed.rss.items.size
        val episodes = latestEpisodes.mapIndexed { index, (episode, pubTime) ->
            val uri = requireNotNull(episode.audio) // Filtered above
            PodcastEpisode(
                uri = uri.toHttps(),
                number = totalCount - index,
                title = episode.title ?: "",
                publicationTime = pubTime,
                feedUri = podcast.feedUri,
                isDownloaded = false,
                fileId = UUID.nameUUIDFromBytes(uri.encodeToByteArray()).toString()
            )
        }
        updatePodcastEpisodes(podcast, feed.title, episodes)
        return latestEpisodes.isNotEmpty()
    }
}
