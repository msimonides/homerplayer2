/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.concurrency.DispatcherProvider
import com.studio4plus.homerplayer2.player.service.PlaybackService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayerViewModel(
    appContext: Context,
    dispatcherProvider: DispatcherProvider,
    audiobooksDao: AudiobooksDao
) : ViewModel() {

    sealed class ViewState {
        object Initializing : ViewState()
        data class Browse(val books: List<AudiobooksDao.AudiobookWithState>) : ViewState()
        object Playing : ViewState()
    }

    private enum class MediaState {
        Initializing,
        Ready,
        Playing
    }

    private var mediaController: MediaController? = null

    // TODO: use a separate class for audiobook-with-state, independent of the DB.
    private val audiobooks: StateFlow<List<AudiobooksDao.AudiobookWithState>> = audiobooksDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), emptyList())

    private val mediaState = MutableStateFlow(MediaState.Initializing)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState: StateFlow<ViewState> = mediaState.flatMapLatest { mediaState ->
        when (mediaState) {
            MediaState.Initializing -> flowOf(ViewState.Initializing)
            MediaState.Ready -> audiobooks.map { books -> ViewState.Browse(books) }
            MediaState.Playing -> flowOf(ViewState.Playing)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.Initializing)

    init {
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        viewModelScope.launch {
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
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        mediaState.value = if (isPlaying) MediaState.Playing else MediaState.Ready
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        super.onIsLoadingChanged(isLoading)
                        Timber.d("Is loading %b", isLoading)
                    }
                }
            )
            mediaState.value = if (mediaController!!.isPlaying) MediaState.Playing else MediaState.Ready
        }
    }

    override fun onCleared() {
        mediaController?.run {
            release()
        }
        super.onCleared()
    }

    fun play(bookId: String) {
        val book = audiobooks.value.find { it.audiobook.id == bookId }
        if (book != null) {
            mediaController?.stop()
            mediaController?.let { controller ->
                controller.setMediaItems(book.toMediaItems())
                controller.playlistMetadata = MediaMetadata.Builder()
                    .setTitle(book.audiobook.displayName)
                    .build()
                if (book.playbackState != null) {
                    controller.seekTo(
                        book.files.indexOfFirst { it.uri == book.playbackState.currentUri },
                        book.playbackState.currentPositionMs
                    )
                }
                controller.playWhenReady = true
                controller.prepare()
            }
        }
    }

    fun stop() {
        mediaController?.playWhenReady = false
        mediaController?.stop()
    }

    private fun AudiobooksDao.AudiobookWithState.toMediaItems() =
        files.map { MediaItem.Builder().setMediaId(it.uri.toString()).build() }
}
