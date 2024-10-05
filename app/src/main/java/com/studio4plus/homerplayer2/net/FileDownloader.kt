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

package com.studio4plus.homerplayer2.net

import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.addHeaderLenient
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.sink
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.resumeWithException

@Factory
class FileDownloader(
    private val dispatcherProvider: DispatcherProvider,
    private val okHttpClient: OkHttpClient,
) {
    @Throws(IOException::class)
    suspend operator fun invoke(file: File, url: String, append: Boolean = false) {
        Timber.i("Start download: $url to ${file.absolutePath}")

        val range = if (append) rangeBytesForFile(file, url) else null
        val requestBuilder = Request.Builder().url(url)
        if (range != null) {
            Timber.i("Download range: $range")
            requestBuilder.addHeader("range", range)
        }
        val call = okHttpClient.newCall(requestBuilder.build())
        val response = call.executeAwait()
        val body = response.body
        Timber.i("Response: ${response.code}")
        runInterruptible(dispatcherProvider.Io) {
            val isSuccess = response.code == 200 || response.code == 206
            val isPartialResponse = response.code == 206
            if (isSuccess && body != null) {
                file.sink(append = isPartialResponse)
                    .buffer()
                    .use { sink -> sink.writeAll(body.source()) }
            } else {
                val beginningOfResponse = body?.source()?.use { it.readUtf8(1000) }
                Timber.w("Response: $beginningOfResponse")
                throw FileNotFoundException("HTTP response code ${response.code}")
            }
        }
    }

    private suspend fun rangeBytesForFile(file: File, url: String): String? {
        val fileExists = withContext(dispatcherProvider.Io) { file.exists() }
        if (!fileExists) return null

        val request = Request.Builder().method("HEAD", null).url(url).build()
        val response = okHttpClient.newCall(request).executeAwait()
        val acceptRanges = response.headers["accept-ranges"]?.lowercase()
        val contentLength = response.headers["content-length"]
        return if (acceptRanges == "bytes") {
            val downloadedBytes = withContext(dispatcherProvider.Io) { file.length() }
            "bytes: $downloadedBytes-$contentLength"
        } else {
            null
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.executeAwait(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    val callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) {
                response.closeQuietly()
            }
        }
    }
    enqueue(callback)
}