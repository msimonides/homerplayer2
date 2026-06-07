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

import com.studio4plus.homerplayer2.base.DispatcherProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@Single
class KtorNetworkClient(
    private val httpClient: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) : NetworkClient {

    override suspend fun getText(
        url: String,
        headers: Map<String, String>,
    ): NetworkResult<String> = execute(url, headers) { response ->
        response.bodyAsText()
    }

    override suspend fun getBytes(
        url: String,
        headers: Map<String, String>,
    ): NetworkResult<ByteArray> = execute(url, headers) { response ->
        response.body()
    }

    override suspend fun head(
        url: String,
        headers: Map<String, String>,
    ): NetworkResult<Unit> {
        return try {
            val response = httpClient.request(normalizeUrl(url)) {
                method = HttpMethod.Head
                headers {
                    applyHeaders(headers)
                }
            }
            val responseHeaders = response.headers.toMap()
            if (response.status.value in 200..299) {
                NetworkResult.Success(response.status.value, responseHeaders, Unit)
            } else {
                NetworkResult.HttpError(response.status.value, responseHeaders)
            }
        } catch (e: Throwable) {
            mapFailure(e)
        }
    }

    override suspend fun downloadToFile(
        url: String,
        target: File,
        append: Boolean,
        rangeStart: Long?,
        headers: Map<String, String>,
    ): NetworkResult<DownloadMeta> {
        return try {
            val statement = httpClient.prepareGet(normalizeUrl(url)) {
                headers {
                    applyHeaders(headers)
                    if (rangeStart != null) {
                        append("Range", "bytes=$rangeStart-")
                    }
                }
            }
            statement.execute { response ->
                val responseHeaders = response.headers.toMap()
                if (response.status.value !in 200..299) {
                    return@execute NetworkResult.HttpError(response.status.value, responseHeaders)
                }

                val bytesWritten = withContext(dispatcherProvider.Io) {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target, append).use { output ->
                        val channel = response.bodyAsChannel()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = channel.readAvailable(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            total += read
                        }
                        output.flush()
                        total
                    }
                }

                NetworkResult.Success(
                    code = response.status.value,
                    headers = responseHeaders,
                    body = DownloadMeta(
                        bytesWritten = bytesWritten,
                        partialContent = response.status.value == 206,
                    )
                )
            }
        } catch (e: Throwable) {
            mapFailure(e)
        }
    }

    private suspend fun <T> execute(
        url: String,
        headers: Map<String, String>,
        bodyMapper: suspend (HttpResponse) -> T,
    ): NetworkResult<T> {
        return try {
            val response = httpClient.request(normalizeUrl(url)) {
                method = HttpMethod.Get
                headers {
                    applyHeaders(headers)
                }
            }
            val responseHeaders = response.headers.toMap()
            if (response.status.value in 200..299) {
                NetworkResult.Success(
                    code = response.status.value,
                    headers = responseHeaders,
                    body = bodyMapper(response),
                )
            } else {
                NetworkResult.HttpError(response.status.value, responseHeaders)
            }
        } catch (e: Throwable) {
            mapFailure(e)
        }
    }

    private fun normalizeUrl(url: String): String = url.toHttps()

    private fun Headers.toMap(): Map<String, String> = names().associateWith { name ->
        getAll(name)?.joinToString(",") ?: ""
    }

    private fun io.ktor.http.HeadersBuilder.applyHeaders(headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            append(name, value)
        }
    }

    private fun mapFailure(e: Throwable): NetworkResult.Failure = when (e) {
        is UnknownHostException -> NetworkResult.Failure(FailureType.UnknownHost, e)
        is SSLException -> NetworkResult.Failure(FailureType.Ssl, e)
        is HttpRequestTimeoutException, is SocketTimeoutException -> NetworkResult.Failure(FailureType.Timeout, e)
        is CancellationException -> NetworkResult.Failure(FailureType.Cancelled, e)
        else -> NetworkResult.Failure(FailureType.Io, e)
    }
}
