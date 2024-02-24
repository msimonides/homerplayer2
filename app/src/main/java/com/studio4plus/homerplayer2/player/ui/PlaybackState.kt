/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.player.Audiobook
import com.studio4plus.homerplayer2.player.service.PlaybackService
import com.studio4plus.homerplayer2.player.usecases.GetBookMediaItemsWithStartPosition
import com.studio4plus.homerplayer2.utils.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Factory
import timber.log.Timber

interface PlaybackController {
    fun seekForward()
    fun seekBack()
    fun seekNext()
    fun seekPrevious()
    fun stop()
}

/**
 * Needs to be stopped after use with a call to shutdown()
 */
@Factory
class PlaybackState(
    mainScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    appContext: Context,
    private val getBookMediaItemsWithStartPosition: GetBookMediaItemsWithStartPosition,
) : PlaybackController {

    sealed interface MediaState {
        data object Initializing : MediaState
        data object Ready : MediaState
        data class Playing(val mediaUri: String, val positionMs: Long) : MediaState
    }

    private val mediaState = MutableStateFlow<MediaState>(MediaState.Initializing)
    private var mediaController: MediaController? = null
    private val eventProgressUpdate = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<MediaState> = mediaState.flatMapLatest {
        when (it) {
            is MediaState.Playing -> tickerFlow(1000)
                .map { mediaController!!.getMediaState() }
                .onStart { emit(it) }
            else -> flowOf(it)
        }
    }
    init {
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        mainScope.launch {
            val builder = MediaController.Builder(appContext, sessionToken)
            mediaController = runInterruptible(dispatcherProvider.Io) {
                builder.buildAsync().get()
            }
            mediaController?.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        Timber.w(error, "Player error")
                    }

                    override fun onPlayerErrorChanged(error: PlaybackException?) {
                        super.onPlayerErrorChanged(error)
                        Timber.w(error, "Player error")
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        Timber.d("Playback state changed %d", playbackState)
                        mediaState.value = mediaController!!.getMediaState()
                        eventProgressUpdate.tryEmit(Unit)
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
                        mediaState.value = mediaController!!.getMediaState()
                    }
                }
            )
            mediaState.value = mediaController!!.getMediaState()
        }
    }

    fun shutdown() {
        mediaController?.run {
            release()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun play(book: Audiobook) {
        mediaController?.stop()
        mediaController?.let { controller ->
            val mediaItemsWithStartPosition =
                getBookMediaItemsWithStartPosition(book, rewindOnResume = true)
            with(mediaItemsWithStartPosition) {
                controller.setMediaItems(mediaItems)
                controller.seekTo(startIndex, startPositionMs)
            }
            controller.playWhenReady = true
            controller.prepare()
        }
    }

    override fun seekForward() {
        mediaController?.seekForward()
    }

    override fun seekBack() {
        mediaController?.seekBack()
    }

    override fun seekNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNext()
            } else {
                // End of last file.
                it.seekTo(it.duration)
            }
        }
    }

    override fun seekPrevious() {
        mediaController?.seekToPrevious()
    }

    override fun stop() {
        mediaController?.playWhenReady = false
        mediaController?.stop()
    }

    private fun MediaController.getMediaState(): MediaState {
        val playingStates = arrayOf(Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_ENDED)
        val mediaItem = currentMediaItem
        return if (playWhenReady && playbackState in playingStates && mediaItem != null)
            MediaState.Playing(mediaItem.mediaId, currentPosition)
        else
            MediaState.Ready
    }
}