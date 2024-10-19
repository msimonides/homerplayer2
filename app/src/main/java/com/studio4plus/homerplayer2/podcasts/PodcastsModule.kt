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

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.studio4plus.homerplayer2.audiobooks.AudiobooksModule
import com.studio4plus.homerplayer2.base.BaseModule
import com.studio4plus.homerplayer2.net.NetModule
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDatabase
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module(includes = [AudiobooksModule::class, BaseModule::class, NetModule::class])
@ComponentScan("com.studio4plus.homerplayer2.podcasts")
class PodcastsModule {
    @Factory
    fun provideRssParser(okHttpClient: OkHttpClient): RssParser =
        RssParserBuilder(callFactory = okHttpClient).build()

    @Factory
    fun providePodcastsDao(db: PodcastsDatabase): PodcastsDao = db.podcastsDao()
}