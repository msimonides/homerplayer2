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

import com.studio4plus.homerplayer2.crash.CrashReporting
import com.studio4plus.homerplayer2.net.NetworkClient
import com.studio4plus.homerplayer2.net.NetworkResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.koin.core.annotation.Factory

private val JSON = Json { ignoreUnknownKeys = true }

@Factory
class GetPopularPodcastITunesIds(
    private val networkClient: NetworkClient,
) {
    @Serializable
    private data class ITunesSearchResults(
        val results: List<ITunesCollection>
    )

    @Serializable
    private data class ITunesCollection(
        val collectionPrice: Double,
        val collectionExplicitness: String? = null,
        val trackId: Long,
        val trackCount: Long,
    )

    @OptIn(ExperimentalSerializationApi::class)
    suspend operator fun invoke(countryCode: String, limit: Int): List<Long> {
        val url = "https://itunes.apple.com/search?media=podcast&term=\"\"&limit=$limit&country=$countryCode"
        val result = networkClient.getBytes(url)
        return if (result is NetworkResult.Success) {
            try {
                val searchResults = result.body.inputStream().use {
                    JSON.decodeFromStream<ITunesSearchResults>(it)
                }
                searchResults.results
                    .filter {
                        it.collectionPrice == 0.0
                                && it.collectionExplicitness.equals("notExplicit", ignoreCase = true)
                                && it.trackCount >= 10
                    }
                    .map { it.trackId }
            } catch (e: IllegalArgumentException) {
                CrashReporting.captureException(e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}