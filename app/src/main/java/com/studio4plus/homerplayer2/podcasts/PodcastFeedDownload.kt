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

package com.studio4plus.homerplayer2.podcasts

import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.net.executeAwait
import kotlinx.coroutines.runInterruptible
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.net.UnknownHostException

@Factory
class PodcastFeedDownload(
    private val dispatcherProvider: DispatcherProvider,
    private val okHttpClient: OkHttpClient
) {
    sealed interface Result {
        data class Success(val body: String) : Result
        data class Error(val httpCode: Int) : Result
        object IncorrectAddress : Result
    }

    suspend operator fun invoke(url: String): Result {
        val httpUrl = url.toHttpUrlOrNull() ?: return Result.IncorrectAddress
        try {
            val request = Request.Builder().url(httpUrl).build()
            val response = okHttpClient.newCall(request).executeAwait()
            Timber.i("$url response: ${response.code}: ${response.message.take(200)}")
            return if (response.isSuccessful) {
                runInterruptible(dispatcherProvider.Io) {
                    val body = response.body
                    if (body != null) {
                        Result.Success(body.string())
                    } else {
                        Timber.w("Empty body")
                        Result.Error(204) // No content
                    }
                }
            } else {
                Result.Error(response.code)
            }
        } catch (e: UnknownHostException) {
            return Result.IncorrectAddress
        }

    }
}