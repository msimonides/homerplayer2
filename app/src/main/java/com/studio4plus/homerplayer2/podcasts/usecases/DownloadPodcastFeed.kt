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

import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.net.executeAwait
import kotlinx.coroutines.runInterruptible
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@Factory
class DownloadPodcastFeed(
    private val dispatcherProvider: DispatcherProvider,
    private val okHttpClient: OkHttpClient,
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
        val httpUrl = url.toHttpUrlOrNull() ?: return Result.UnknownAddress
        try {
            val request = Request.Builder().url(httpUrl).build()
            val response = okHttpClient.newCall(request).executeAwait()
            Timber.i("$url response: ${response.code}: ${response.message.take(200)}")
            return if (response.isSuccessful) {
                val body = runInterruptible(dispatcherProvider.Io) { response.body?.string() }
                if (body != null) {
                    val podcastFeed = parsePodcastFeed(body, url)
                    if (podcastFeed != null)
                        Result.Success(podcastFeed)
                    else
                        Result.ParseError
                } else {
                    Timber.w("Empty body")
                    Result.Error(204) // No content
                }
            } else {
                Result.Error(response.code)
            }
        } catch (e: UnknownHostException) {
            return Result.UnknownAddress
        } catch (e: SSLException) {
            Timber.w(e, "Error fetching RSS")
            return Result.SslError
        } catch (e: IOException) {
            Timber.i(e, "Error fetching RSS")
            return Result.IoError
        }
    }
}