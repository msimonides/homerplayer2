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

package com.studio4plus.homerplayer2.testutils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule.Companion.MAX_SEEK_TO_PREVIOUS_POSITION
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule.Companion.SEEK_BACK_INCREMENT_MS
import com.studio4plus.homerplayer2.exoplayer.ExoplayerModule.Companion.SEEK_FORWARD_INCREMENT_MS
import com.studio4plus.homerplayer2.player.PlaybackUiState
import com.studio4plus.homerplayer2.player.PlayerModule
import com.studio4plus.homerplayer2.player.usecases.BuildMediaController
import com.studio4plus.homerplayer2.podcasts.PodcastsTaskScheduler
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.mock.declare

fun KoinTest.declareFakes() {
    val testDispatcher = StandardTestDispatcher()
    val testScope = TestScope(testDispatcher)
    declare<Context> { ApplicationProvider.getApplicationContext() }
    declare<TestScope> { testScope }
    declare<DispatcherProvider> { TestDispatcherProvider(testDispatcher) }
    declare<Clock> { TestScopeClock(testScope) }
    declare<AppDatabase> { createInMemoryDatabase() }
    declare<CoroutineScope> { testScope.backgroundScope }
    declare<ExoPlayer> {
        TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(FakeMediaSourceFactory())
            // Unfortunately TestExoPlayerBuilder is not an ExoPlayer.Builder and the configuration
            // code cannot be directly shared.
            .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT_MS)
            .setSeekBackIncrementMs(SEEK_BACK_INCREMENT_MS)
            .setMaxSeekToPreviousPositionMs(MAX_SEEK_TO_PREVIOUS_POSITION)
            .build()
    }
    declare<BuildMediaController> { TestBuildMediaController(get<ExoPlayer>()) }
    declare<DataStore<PlaybackUiState>>(named(PlayerModule.UI_STATE)) { FakeDataStore(PlaybackUiState()) }
    declare<PodcastsTaskScheduler> { FakePodcastsTaskScheduler() }
}