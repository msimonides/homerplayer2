/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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

import java.io.File

interface NetworkClient {
    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResult<String>

    suspend fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResult<ByteArray>

    suspend fun head(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResult<Unit>

    suspend fun postXml(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResult<String>

    suspend fun downloadToFile(
        url: String,
        target: File,
        append: Boolean = false,
        rangeStart: Long? = null,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResult<DownloadMeta>
}

data class DownloadMeta(
    val bytesWritten: Long,
    val partialContent: Boolean,
)

sealed interface NetworkResult<out T> {
    data class Success<T>(
        val code: Int,
        val headers: Map<String, List<String>>,
        val body: T,
    ) : NetworkResult<T>

    data class HttpError(
        val code: Int,
        val headers: Map<String, List<String>>,
    ) : NetworkResult<Nothing>

    data class Failure(
        val type: FailureType,
        val cause: Throwable? = null,
    ) : NetworkResult<Nothing>
}

enum class FailureType {
    UnknownHost,
    Ssl,
    Timeout,
    Io,
}

