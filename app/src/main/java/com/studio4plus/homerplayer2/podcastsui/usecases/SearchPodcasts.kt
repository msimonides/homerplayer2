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

package com.studio4plus.homerplayer2.podcastsui.usecases

import com.mr3y.podcastindex.PodcastIndexClient
import com.mr3y.podcastindex.model.RateLimitExceededException
import com.studio4plus.homerplayer2.net.toHttps
import com.studio4plus.homerplayer2.podcastsui.PODCAST_SEARCH_MAX_RESULTS
import io.sentry.Sentry
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.util.concurrent.CancellationException

data class PodcastSearchResult(
    val feedUri: String,
    val title: String,
    val author: String,
    val description: String,
    val artworkUri: String,
)

@Factory
class SearchPodcasts (
    private val podcastIndex: PodcastIndexClient
) {
    sealed interface Result {
        data class Success(val results: List<PodcastSearchResult>) : Result
        object RateLimitError : Result
        object RequestError : Result
    }

    suspend operator fun invoke(searchTerm: String): Result =
        try {
            val results = podcastIndex.search.forPodcastsByTerm(
                searchTerm,
                limit = PODCAST_SEARCH_MAX_RESULTS
            )
            val searchResults = results.feeds
                .filter { it.dead == 0 }
                .map {
                    PodcastSearchResult(
                        feedUri = it.url,
                        title = it.title,
                        author = it.author,
                        description = it.description,
                        artworkUri = it.image.toHttps(),
                    )
                }
            Result.Success(searchResults)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RateLimitExceededException) {
            Sentry.captureException(e)
            Result.RateLimitError
        } catch (e: Exception) {
            Sentry.captureException(e)
            Timber.e(e, "search request error")
            Result.RequestError
        }
}
