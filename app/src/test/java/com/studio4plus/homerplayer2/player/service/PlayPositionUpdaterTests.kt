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

package com.studio4plus.homerplayer2.player.service

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import com.studio4plus.homerplayer2.app.AppModule
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.testdata.TestData
import com.studio4plus.homerplayer2.testutils.declareFakes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayPositionUpdaterTests : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(AppModule().module)
        declareFakes()
    }

    val audiobooksDao: AudiobooksDao by inject()
    val player: ExoPlayer by inject()
    val testScope: TestScope by inject()

    val positionUpdater: PlayPositionUpdater by inject()

    @Before
    fun setup() {
        runBlocking {
            insertAudiobooks()
        }
        player.addListener(positionUpdater)
    }

    @Test
    fun `when playback stops then positon is saved`() {
        player.setMediaItem(TestData.audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 501)
        testScope.advanceTimeBy(500)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookWithState = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(TestData.audiobookFile1.uri, audiobookWithState?.playbackState?.currentUri)
        assertEquals(500, audiobookWithState?.playbackState?.currentPositionMs)
    }

    @Test
    fun `when playing then position is saved every 5 seconds`() {
        player.setMediaItem(TestData.audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 5001)
        testScope.advanceTimeBy(5000)

        val audiobookWithState5s = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(TestData.audiobookFile1.uri, audiobookWithState5s?.playbackState?.currentUri)
        assertEquals(5000, audiobookWithState5s?.playbackState?.currentPositionMs)

        TestPlayerRunHelper.play(player).untilPosition(0, 10001)
        testScope.advanceTimeBy(5000)

        val audiobookWithState10s = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(TestData.audiobookFile1.uri, audiobookWithState10s?.playbackState?.currentUri)
        assertEquals(10000, audiobookWithState10s?.playbackState?.currentPositionMs)
    }

    @Test
    fun `when audiobook is played below 15s then it's still new`() {
        player.setMediaItem(TestData.audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 5001)
        testScope.advanceTimeBy(5000)

        val audiobookWhilePlaying = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(true, audiobookWhilePlaying?.playbackState?.isNew)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookAfterStopping = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(true, audiobookAfterStopping?.playbackState?.isNew)
    }

    @Test
    fun `when audiobook is played above 15s then it's not-new`() {
        player.setMediaItem(TestData.audiobookFile2.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 17001)
        testScope.advanceTimeBy(17000)

        val audiobookWhilePlaying = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(false, audiobookWhilePlaying?.playbackState?.isNew)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookAfterStopping = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(false, audiobookAfterStopping?.playbackState?.isNew)
    }

    @Test
    fun `when not-new audiobook is rewound below 15s then it's not-new`() {
        player.setMediaItem(TestData.audiobookFile2.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 17_001)
        testScope.advanceTimeBy(17_000)

        player.seekTo(0, 1000)
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobook = runBlocking { audiobooksDao.getAudiobook(TestData.audiobook.id).first() }
        assertEquals(1000, audiobook?.playbackState?.currentPositionMs)
        assertEquals(false, audiobook?.playbackState?.isNew)
    }

    private suspend fun insertAudiobooks() {
        audiobooksDao.insertAudiobook(TestData.audiobook, listOf(TestData.audiobookFile1, TestData.audiobookFile2))
        audiobooksDao.insertAudiobookFileDurations(listOf(TestData.audiobookFileDuration1, TestData.audiobookFileDuration2))
    }

    private fun AudiobookFile.toMediaItem() = MediaItem.Builder().setUri(uri).build()
}