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
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.studio4plus.homerplayer2.core.DispatcherProvider
import com.studio4plus.homerplayer2.player.service.PlaybackService
import com.studio4plus.homerplayer2.utils.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    appContext: Context
) : PlaybackController {

    enum class MediaState {
        Initializing,
        Ready,
        Playing
    }

    private val mediaState = MutableStateFlow(MediaState.Initializing)
    private var mediaController: MediaController? = null
    private val eventProgressUpdate = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val state: Flow<MediaState>
        get() = mediaState
    val progressFlow = playedBookProgressFlow()

    init {
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        mainScope.launch {
            val builder = MediaController.Builder(appContext, sessionToken)
            mediaController = withContext(dispatcherProvider.Io) {
                @Suppress("BlockingMethodInNonBlockingContext")
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
                        mediaState.value = mediaStateFor(playbackState, mediaController!!.playWhenReady)
                        eventProgressUpdate.tryEmit(Unit)
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
                        mediaState.value = mediaStateFor(mediaController!!.playbackState, playWhenReady)
                    }
                }
            )
            mediaState.value = mediaStateFor(mediaController!!.playbackState, mediaController!!.playWhenReady)
        }
    }

    fun shutdown() {
        mediaController?.run {
            release()
        }
    }

    fun play(book: Audiobook) {
        mediaController?.stop()
        mediaController?.let { controller ->
            controller.setMediaItems(book.toMediaItems())
            controller.playlistMetadata = MediaMetadata.Builder()
                .setTitle(book.displayName)
                .build()
            if (book.currentUri != null) {
                controller.seekTo(
                    book.files.indexOfFirst { it.uri == book.currentUri },
                    book.currentPositionMs
                )
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

    private fun playedBookProgressFlow(): Flow<Pair<Uri, Long>?> = merge(
        eventProgressUpdate,
        tickerFlow(1000) // TODO: more precise interval, take account of playback speed and total duration (for very short books).
    ).map {
        mediaController?.let { controller ->
            val mediaUri = controller.currentMediaItem?.mediaId.let { Uri.parse(it) }
            val position = controller.contentPosition
            Pair(mediaUri, position)
        }
    }

    private fun Audiobook.toMediaItems() =
        files.map { MediaItem.Builder().setMediaId(it.uri.toString()).build() }

    private fun mediaStateFor(playbackState: Int, playWhenReady: Boolean) =
        if (playWhenReady && playbackState in arrayOf(Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_ENDED))
            MediaState.Playing
        else
            MediaState.Ready
}