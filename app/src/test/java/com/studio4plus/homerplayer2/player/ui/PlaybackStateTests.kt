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

package com.studio4plus.homerplayer2.player.ui

import android.app.Application
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import com.studio4plus.homerplayer2.app.AppModule
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.player.toAudiobook
import com.studio4plus.homerplayer2.testdata.TestData
import com.studio4plus.homerplayer2.testutils.declareFakes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlaybackStateTests : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(AppModule().module)
        declareFakes()
    }

    private val audiobooksDao: AudiobooksDao by inject()
    private val player: ExoPlayer by inject()
    private val testScope: TestScope by inject()

    private val playbackState: PlaybackState by inject()

    @Before
    fun setup() {
        runBlocking { insertAudiobooks() }
    }

    @Test
    fun `seekBack rewind tests`() = testScope.runTest {
        class Position(val mediaItem: Int, val positionMs: Long)

        val testData = listOf(
            Position(1, 45_000) to Position(1, 15_000),
            Position(1, 40_000) to Position(1, 0),
            Position(1, 20_000) to Position(1, 0),
            // TODO: fix the test for going back to previous item:
            //Position(1, 10_000) to Position(0, 40_000)
            Position(0, 10_000) to Position(0, 0),

        )

        // Initialize playbackState and create MediaController.
        playbackState
        runCurrent()

        val uiAudiobook = audiobooksDao.getAudiobook(TestData.audiobook.id).first()?.toAudiobook()
        assertNotNull(uiAudiobook)

        playbackState.play(uiAudiobook)
        runCurrent()
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()

        testData.forEachIndexed { index, (initial, expected) ->
            player.seekTo(initial.mediaItem, initial.positionMs)
            TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()

            playbackState.seekBack()
            runCurrent()
            TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()

            assertEquals(expected.mediaItem, player.currentMediaItemIndex, "test $index")
            assertEquals(expected.positionMs, player.currentPosition, "test $index")
        }
    }

    private suspend fun insertAudiobooks() {
        audiobooksDao.insertAudiobook(TestData.audiobook, listOf(TestData.audiobookFile1, TestData.audiobookFile2))
        audiobooksDao.insertAudiobookFileDurations(listOf(TestData.audiobookFileDuration1, TestData.audiobookFileDuration2))
    }
}