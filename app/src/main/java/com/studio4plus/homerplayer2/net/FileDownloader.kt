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
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@Factory
class FileDownloader(
    private val dispatcherProvider: DispatcherProvider,
    private val networkClient: NetworkClient,
) {
    @Throws(IOException::class)
    suspend operator fun invoke(file: File, url: String, append: Boolean = false) {
        Timber.i("Start download: $url to ${file.absolutePath}")

        val range = if (append) rangeBytesForFile(file, url) else null
        if (range?.isFinished == true) {
            Timber.i("File already fully downloaded")
            return
        }

        val rangeStart = range?.downloadedBytes
        if (rangeStart != null) {
            Timber.i("Download range: bytes=$rangeStart-")
        }

        when (val result = networkClient.downloadToFile(url, file, append = rangeStart != null, rangeStart = rangeStart)) {
            is NetworkResult.Success -> {
                Timber.i("Finished download of $url (${result.body.bytesWritten} bytes, partial=${result.body.partialContent})")
            }
            is NetworkResult.HttpError -> {
                throw FileNotFoundException("HTTP response code ${result.code}")
            }
            is NetworkResult.Failure -> {
                throw IOException("Download failed: ${result.type}", result.cause)
            }
        }
    }

    private suspend fun rangeBytesForFile(file: File, url: String): DownloadRange? {
        val fileExists = withContext(dispatcherProvider.Io) { file.exists() }
        if (!fileExists) return null

        val result = networkClient.head(url)
        val headers = when (result) {
            is NetworkResult.Success -> result.headers
            is NetworkResult.HttpError, is NetworkResult.Failure -> return null
        }
        val acceptRanges = headers["accept-ranges"]?.lowercase()
        val contentLength = headers["content-length"]?.toLong()
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