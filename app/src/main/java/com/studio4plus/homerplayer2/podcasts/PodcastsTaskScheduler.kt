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

import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single

interface PodcastsTaskScheduler {
    fun enablePeriodicUpdate()
    fun disablePeriodicUpdate()
    fun runUpdate()
}

@Single(createdAtStart = true)
class PodcastRefreshScheduler(
    mainScope: CoroutineScope,
    podcastsDao: PodcastsDao,
    scheduler: PodcastsTaskScheduler,
) {
    init {
        // TODO: open for some future app start optimizations
        podcastsDao.observeHasAnyPodcast()
            .distinctUntilChanged()
            .onEach { hasPodcasts ->
                when (hasPodcasts) {
                    true -> scheduler.enablePeriodicUpdate()
                    false -> scheduler.disablePeriodicUpdate()
                }
            }.launchIn(mainScope)
    }
}