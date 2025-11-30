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

package com.studio4plus.homerplayer2.podcastsui.usecases

import com.mr3y.podcastindex.PodcastIndexClient
import com.mr3y.podcastindex.model.SinglePodcastResult
import com.studio4plus.homerplayer2.net.toHttps
import com.studio4plus.homerplayer2.podcastsui.PODCAST_SEARCH_MAX_RESULTS
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class GetPopularPodcasts(
    private val getPopularPodcastITunesIds: GetPopularPodcastITunesIds,
    private val podcastIndex: PodcastIndexClient
) {
    private data class CachedResult(
        val countryCode: String,
        val results: List<PodcastSearchResult>
    )

    private var cachedResults: CachedResult? = null

    suspend operator fun invoke(countryCode: String): List<PodcastSearchResult> =
        cachedResults
            ?.takeIf { it.countryCode.equals(countryCode, ignoreCase = true) }
            ?.results
            ?: fetchPopularPodcasts(countryCode).also {
                storeInCache(countryCode, it)
            }

    private fun storeInCache(countryCode: String, results: List<PodcastSearchResult>) {
        if (results.isNotEmpty()) {
            cachedResults = CachedResult(countryCode, results)
        }
    }

    private suspend fun fetchPopularPodcasts(countryCode: String): List<PodcastSearchResult> {
        // Take a few more in case some are not available.
        val popularPodcastIds = getPopularPodcastITunesIds(countryCode, PODCAST_SEARCH_MAX_RESULTS + 3)
        val results = coroutineScope {
            popularPodcastIds
                .map { id -> async { fetchPodcastByITunesId(id) } }
                .awaitAll()
                .filterNotNull()
                .take(PODCAST_SEARCH_MAX_RESULTS)
        }

        return results
            .map {
                PodcastSearchResult(
                    feedUri = it.feed.url,
                    title = it.feed.title,
                    author = it.feed.author,
                    description = it.feed.description,
                    artworkUri = it.feed.image.toHttps(),
                )
            }
    }

    private suspend fun fetchPodcastByITunesId(id: Long): SinglePodcastResult? =
        try {
            podcastIndex.podcasts.byItunesId(id)
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            Sentry.captureException(e)
            null
        }
}