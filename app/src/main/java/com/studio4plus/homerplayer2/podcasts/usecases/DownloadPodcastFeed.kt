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

import com.studio4plus.homerplayer2.net.FailureType
import com.studio4plus.homerplayer2.net.NetworkClient
import com.studio4plus.homerplayer2.net.NetworkResult
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import org.koin.core.annotation.Factory
import timber.log.Timber

@Factory
class DownloadPodcastFeed(
    private val networkClient: NetworkClient,
    private val parsePodcastFeed: ParsePodcastFeed,
) {

    sealed interface Result {
        data class Success(val feed: PodcastFeed) : Result
        object ParseError : Result
        data class Error(val httpCode: Int) : Result
        object UnknownAddress : Result
        object SslError : Result
        object IoError : Result
    }

    suspend operator fun invoke(url: String): Result {
        if (!isHttpUrl(url)) return Result.UnknownAddress

        return when (val result = networkClient.getText(url)) {
            is NetworkResult.Success -> {
                Timber.i("$url response: ${result.code}")
                if (result.code == 204) {
                    Result.Error(204)
                } else {
                    val podcastFeed = parsePodcastFeed(result.body, url)
                    if (podcastFeed != null) {
                        Result.Success(podcastFeed)
                    } else {
                        Result.ParseError
                    }
                }
            }

            is NetworkResult.HttpError -> {
                Timber.i("$url response: ${result.code}")
                Result.Error(result.code)
            }

            is NetworkResult.Failure -> {
                when (result.type) {
                    FailureType.UnknownHost -> Result.UnknownAddress
                    FailureType.Ssl -> {
                        Timber.w(result.cause, "Error fetching RSS")
                        Result.SslError
                    }
                    FailureType.Timeout,
                    FailureType.Io -> {
                        Timber.i(result.cause, "Error fetching RSS")
                        Result.IoError
                    }
                }
            }
        }
    }

    private fun isHttpUrl(url: String): Boolean = runCatching {
        val parsed = Url(url)
        parsed.host.isNotBlank() && (
            parsed.protocol == URLProtocol.HTTP || parsed.protocol == URLProtocol.HTTPS
        )
    }.getOrDefault(false)
}
