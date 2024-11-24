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

package com.studio4plus.homerplayer2.podcastsui

import android.content.Context
import com.mr3y.podcastindex.ktor3.PodcastIndexClient
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.podcasts.PodcastsModule
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module(
    includes = [PodcastsModule::class]
)
@ComponentScan("com.studio4plus.homerplayer2.podcastsui")
class PodcastsUiModule {

    @Factory
    fun providePodcastIndexClient(appContext: Context, okHttpClient: OkHttpClient) =
        PodcastIndexClient(
            appContext.getString(R.string.podcastindex_key),
            appContext.getString(R.string.podcastindex_secret),
            "HomerPlayer2/${BuildConfig.VERSION_NAME}"
        ) {
            httpClient(OkHttp) {
                engine { preconfigured = okHttpClient }
            }
        }
}