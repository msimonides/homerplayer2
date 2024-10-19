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
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@Factory
class FileDownloader(
    private val dispatcherProvider: DispatcherProvider,
    private val okHttpClient: OkHttpClient,
) {
    @Throws(IOException::class)
    suspend operator fun invoke(file: File, url: String, append: Boolean = false) {
        Timber.i("Start download: $url to ${file.absolutePath}")

        val range = if (append) rangeBytesForFile(file, url) else null
        if (range?.isFinished == true) {
            Timber.i("File already fully downloaded")
            return
        }

        val requestBuilder = Request.Builder().url(url)
        if (range != null) {
            val rangeHeader = "bytes: ${range.downloadedBytes}-${range.totalBytes}"
            Timber.i("Download range: $rangeHeader")
            requestBuilder.addHeader("range", rangeHeader)
        }
        val call = okHttpClient.newCall(requestBuilder.build())
        val response = call.executeAwait()
        val body = response.body
        Timber.i("Response: ${response.code}: ${response.message.take(200)}")
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

    private suspend fun rangeBytesForFile(file: File, url: String): DownloadRange? {
        val fileExists = withContext(dispatcherProvider.Io) { file.exists() }
        if (!fileExists) return null

        val request = Request.Builder().method("HEAD", null).url(url).build()
        val response = okHttpClient.newCall(request).executeAwait()
        val acceptRanges = response.headers["accept-ranges"]?.lowercase()
        val contentLength = response.headers["content-length"]?.toLong()
        return if (acceptRanges == "bytes" && contentLength != null) {
            val downloadedBytes = withContext(dispatcherProvider.Io) { file.length() }
            DownloadRange(downloadedBytes, contentLength)
        } else {
            null
        }
    }
    private data class DownloadRange(val downloadedBytes: Long, val totalBytes: Long) {
        val isFinished = downloadedBytes == totalBytes
    }
}