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
import android.media.AudioManager
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.studio4plus.homerplayer2.app.data.UiSettings
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.core.DispatcherProvider
import com.studio4plus.homerplayer2.player.PlaybackUiStateRepository
import com.studio4plus.homerplayer2.player.service.PlaybackService
import com.studio4plus.homerplayer2.settings.DATASTORE_UI_SETTINGS
import com.studio4plus.homerplayer2.speech.Speaker
import com.studio4plus.homerplayer2.utils.tickerFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named
import timber.log.Timber

@KoinViewModel
class PlayerViewModel(
    appContext: Context,
    dispatcherProvider: DispatcherProvider,
    audiobooksDao: AudiobooksDao,
    @Named(DATASTORE_UI_SETTINGS) uiSettingsStore: DataStore<UiSettings>,
    private val playbackUiStateRepository: PlaybackUiStateRepository,
    private val audioManager: AudioManager,
    private val speaker: Speaker,
) : ViewModel(), DefaultLifecycleObserver {

    data class AudiobookState(
        val id: String,
        val displayName: String,
        val progress: Float
    )

    sealed class ViewState {
        object Initializing : ViewState()
        data class Browse(
            val books: List<AudiobookState>,
            val initialSelectedIndex: Int
        ) : ViewState()
        data class Playing(val progress: Float) : ViewState()
    }

    private enum class MediaState {
        Initializing,
        Ready,
        Playing
    }

    private var mediaController: MediaController? = null

    private val audiobooks: Flow<List<Audiobook>> = audiobooksDao.getAll()
        .map { books -> books.map { it.toAudiobook() } }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val mediaState = MutableStateFlow(MediaState.Initializing)

    // TODO: there should be a better way to expose the currently played book
    private var playedAudiobook: Audiobook? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState: StateFlow<ViewState> = mediaState.flatMapLatest { mediaState ->
        when (mediaState) {
            MediaState.Initializing -> flowOf(ViewState.Initializing)
            MediaState.Ready -> audiobooksBrowseStateFlow()
            MediaState.Playing -> playedBookProgressFlow().map { ViewState.Playing(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ViewState.Initializing)

    private val uiSettings = uiSettingsStore.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings.getDefaultInstance())

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

    override fun onStart(owner: LifecycleOwner) {
        viewModelScope.launch {
            speaker.initIfNeeded()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        speaker.shutdown()
    }

    override fun onCleared() {
        mediaController?.run {
            release()
        }
        super.onCleared()
    }

    fun play(bookIndex: Int) {
        viewModelScope.launch {
            playedAudiobook = audiobooks.first().getOrNull(bookIndex)
            val book = playedAudiobook
            if (book != null) {
                speaker.stop()
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
        }
    }

    fun onPageChanged(bookIndex: Int) {
        viewModelScope.launch {
            val book = audiobooks.first().getOrNull(bookIndex)
            speaker.stop()
            if (book != null) {
                if (uiSettings.value.readBookTitles) {
                    viewModelScope.launch {
                        speaker.speakAndWait(book.displayName)
                    }
                }
                playbackUiStateRepository.updateLastSelectedBookId(book.id)
            }
        }
    }

    fun seekForward() {
        mediaController?.seekForward()
    }

    fun seekBack() {
        mediaController?.seekBack()
    }

    fun seekNext() {
        mediaController?.seekToNext()
    }

    fun seekPrevious() {
        mediaController?.seekToPrevious()
    }

    fun stop() {
        mediaController?.playWhenReady = false
        mediaController?.stop()
    }

    fun volumeUp() {
        // TODO: implement UI for showing volume changes instead of FLAG_SHOW_UI
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
        )
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
        )
    }

    private fun Audiobook.toMediaItems() =
        files.map { MediaItem.Builder().setMediaId(it.uri.toString()).build() }

    private fun List<Audiobook>.toBrowsable() =
        map { with(it) { AudiobookState(id, displayName, progress) } }

    private fun audiobooksBrowseStateFlow(): Flow<ViewState.Browse> =
        combine(
            audiobooks,
            playbackUiStateRepository.lastSelectedBookId()
        ) { books, lastSelectedId ->
            val initialSelectedIndex =
                books.indexOfFirst { it.id == lastSelectedId }.coerceAtLeast(0)
            ViewState.Browse(books.toBrowsable(), initialSelectedIndex)
        }

    private val eventProgressUpdate = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun playedBookProgressFlow(): Flow<Float> = merge(
        eventProgressUpdate,
        tickerFlow(1000) // TODO: more precise interval, take account of playback speed and total duration (for very short books).
    ).map {
        mediaController?.let { controller ->
            val mediaUri = controller.currentMediaItem?.mediaId.let { Uri.parse(it) }
            val position = controller.contentPosition
            val audiobookWithCurrentProgress =
                playedAudiobook?.copy(currentUri = mediaUri, currentPositionMs = position)
            audiobookWithCurrentProgress?.progress ?: 0f
        } ?: 0f
    }

    private fun mediaStateFor(playbackState: Int, playWhenReady: Boolean) =
        if (playWhenReady && playbackState in arrayOf(Player.STATE_BUFFERING, Player.STATE_READY))
            MediaState.Playing
        else
            MediaState.Ready
}
