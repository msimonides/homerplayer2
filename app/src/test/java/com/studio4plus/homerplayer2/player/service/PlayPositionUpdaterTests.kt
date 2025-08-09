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
import android.net.Uri
import androidx.media3.common.AdPlaybackState
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeRenderer
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.FakeTimeline.TimelineWindowDefinition
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobookFileDuration
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.player.PlaybackUiState
import com.studio4plus.homerplayer2.player.PlaybackUiStateRepository
import com.studio4plus.homerplayer2.testutils.FakeDataStore
import com.studio4plus.homerplayer2.testutils.createInMemoryDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlayPositionUpdaterTests {

    private lateinit var db: AppDatabase
    private lateinit var audiobooksDao: AudiobooksDao
    private lateinit var playbackUiStateRepository: PlaybackUiStateRepository
    private lateinit var player: ExoPlayer
    private lateinit var testScope: TestScope

    private lateinit var positionUpdater: PlayPositionUpdater

    private val audiobook = Audiobook(
        id = "audiobook1",
        displayName = "Audiobook 1",
        rootFolderUri = Uri.parse("content://audiobook1")
    )
    private val audiobookFile1 = AudiobookFile(
        bookId = audiobook.id,
        uri = Uri.parse("content://audiobook1/1")
    )
    private val audiobookFile2 = AudiobookFile(
        bookId = audiobook.id,
        uri = Uri.parse("content://audiobook1/1")
    )
    private val audiobookFileDuration1 = AudiobookFileDuration(audiobookFile1.uri, 15_000)
    private val audiobookFileDuration2 = AudiobookFileDuration(audiobookFile2.uri, 20_000)
    private val mediaDurationsMs =
        listOf(audiobookFileDuration1, audiobookFileDuration2).associate { it.uri to it.durationMs }

    @Before
    fun setup() {
        db = createInMemoryDatabase()
        audiobooksDao = db.audiobooksDao()

        testScope = TestScope()

        playbackUiStateRepository =
            PlaybackUiStateRepository(testScope.backgroundScope, FakeDataStore(PlaybackUiState()))
        positionUpdater =
            PlayPositionUpdater(testScope.backgroundScope, audiobooksDao, playbackUiStateRepository)
        runBlocking {
            insertAudiobooks()
        }
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(FakeMediaSourceFactory(mediaDurationsMs))
            .setRenderers(FakeRenderer(C.TRACK_TYPE_AUDIO))
            .build()
        player.addListener(positionUpdater)
    }

    @Test
    fun `when playback stops then positon is saved`() {
        player.setMediaItem(audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 501)
        testScope.advanceTimeBy(500)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookWithState = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(audiobookFile1.uri, audiobookWithState?.playbackState?.currentUri)
        assertEquals(500, audiobookWithState?.playbackState?.currentPositionMs)
    }

    @Test
    fun `when playing then position is saved every 5 seconds`() {
        player.setMediaItem(audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 5001)
        testScope.advanceTimeBy(5000)

        val audiobookWithState5s = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(audiobookFile1.uri, audiobookWithState5s?.playbackState?.currentUri)
        assertEquals(5000, audiobookWithState5s?.playbackState?.currentPositionMs)

        TestPlayerRunHelper.play(player).untilPosition(0, 10001)
        testScope.advanceTimeBy(5000)

        val audiobookWithState10s = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(audiobookFile1.uri, audiobookWithState10s?.playbackState?.currentUri)
        assertEquals(10000, audiobookWithState10s?.playbackState?.currentPositionMs)
    }

    @Test
    fun `when audiobook is played below 15s then it's still new`() {
        player.setMediaItem(audiobookFile1.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 5001)
        testScope.advanceTimeBy(5000)

        val audiobookWhilePlaying = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(true, audiobookWhilePlaying?.playbackState?.isNew)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookAfterStopping = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(true, audiobookAfterStopping?.playbackState?.isNew)
    }

    @Test
    fun `when audiobook is played above 15s then it's not-new`() {
        player.setMediaItem(audiobookFile2.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 17001)
        testScope.advanceTimeBy(17000)

        val audiobookWhilePlaying = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(false, audiobookWhilePlaying?.playbackState?.isNew)

        player.playWhenReady = false
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobookAfterStopping = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(false, audiobookAfterStopping?.playbackState?.isNew)
    }

    @Test
    fun `when not-new audiobook is rewound below 15s then it's not-new`() {
        player.setMediaItem(audiobookFile2.toMediaItem())
        player.prepare()

        TestPlayerRunHelper.play(player).untilPosition(0, 17_001)
        testScope.advanceTimeBy(17_000)

        player.seekTo(0, 1000)
        TestPlayerRunHelper.advance(player).untilPendingCommandsAreFullyHandled()
        testScope.runCurrent()

        val audiobook = runBlocking { audiobooksDao.getAudiobook(audiobook.id).first() }
        assertEquals(1000, audiobook?.playbackState?.currentPositionMs)
        assertEquals(false, audiobook?.playbackState?.isNew)
    }

    private suspend fun insertAudiobooks() {
        audiobooksDao.insertAudiobook(audiobook, listOf(audiobookFile1, audiobookFile2))
        audiobooksDao.insertAudiobookFileDurations(listOf(audiobookFileDuration1, audiobookFileDuration2))
    }

    private fun AudiobookFile.toMediaItem() = MediaItem.Builder().setUri(uri).build()
}

private class FakeMediaSourceFactory(private val durationsMs : Map<Uri, Long>) : MediaSource.Factory {
    private val DEFAULT_UID = Unit

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        throw UnsupportedOperationException()
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        throw UnsupportedOperationException()
    }

    override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri
        val durationMs = durationsMs[uri]
        checkNotNull(durationMs)
        val timelineWindowDefinition = TimelineWindowDefinition(
            /* periodCount = */ 1,
            /* id = */ DEFAULT_UID,
            /* isSeekable = */ true,
            /* isDynamic = */ false,
            /* isLive = */ false,
            /* isPlaceholder = */ false,
            /* durationUs = */ durationMs * C.MICROS_PER_SECOND,
            /* defaultPositionUs = */ 0L,
            /* windowOffsetInFirstPeriodUs = */ 0L,
            /* adPlaybackStates = */ mutableListOf(AdPlaybackState.NONE),
            /* mediaItem = */ mediaItem
        )
        return FakeMediaSource(FakeTimeline(timelineWindowDefinition))
    }

}