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

import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.battery.BatteryState
import com.studio4plus.homerplayer2.battery.BatteryStateProvider
import com.studio4plus.homerplayer2.player.Audiobook
import com.studio4plus.homerplayer2.player.AudiobookFile
import com.studio4plus.homerplayer2.player.PlaybackUiStateRepository
import com.studio4plus.homerplayer2.player.toAudiobook
import com.studio4plus.homerplayer2.settings.DATASTORE_UI_SETTINGS
import com.studio4plus.homerplayer2.settings.UiSettings
import com.studio4plus.homerplayer2.speech.Speaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class PlayerViewModel(
    private val playbackState: PlaybackState,
    audiobooksDao: AudiobooksDao,
    @Named(DATASTORE_UI_SETTINGS) uiSettingsStore: DataStore<UiSettings>,
    private val playbackUiStateRepository: PlaybackUiStateRepository,
    private val audioManager: AudioManager,
    private val speaker: Speaker,
    batteryStateProvider: BatteryStateProvider,
) : ViewModel(), PlaybackController by playbackState, DefaultLifecycleObserver {

    data class UiAudiobook(
        val id: String,
        val displayName: String,
        val progress: Float
    )

    data class AllUiAudiobooks(
        val bookStates: List<UiAudiobook>,
        val books: List<Audiobook>,
        val selectedIndex: Int
    )

    sealed interface BooksState {
        object Initializing : BooksState
        data class Books(
            val books: List<UiAudiobook>,
            val selectedIndex: Int,
            val isPlaying: Boolean
        ) : BooksState
    }

    private val allUiBooks: Flow<AllUiAudiobooks> = combine(
        audiobooksDao.getAll().map { books -> books.map { it.toAudiobook() } },
        playbackUiStateRepository.lastSelectedBookId()
    ) { books, lastSelectedId ->
        val lastSelectedIndex =
            books.indexOfFirst { it.id == lastSelectedId }.coerceAtLeast(0)
        AllUiAudiobooks(books.toUiBook(), books, lastSelectedIndex)
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val playbackStateFlow = playbackState.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState.MediaState.Initializing)

    val booksState: StateFlow<BooksState> = combine(
        allUiBooks,
        playbackStateFlow
    ) { booksState, mediaState ->
        when (mediaState) {
            is PlaybackState.MediaState.Initializing -> BooksState.Initializing
            is PlaybackState.MediaState.Ready ->
                BooksState.Books(booksState.bookStates, booksState.selectedIndex, isPlaying = false)
            is PlaybackState.MediaState.Playing -> {
                // TODO: refactor
                // Store the matching file to get its Uri instead of parsing it from string.
                var matchingFile: AudiobookFile? = null
                val playingBook = booksState.books.find {
                    matchingFile = it.files.find { f -> f.uri.toString() == mediaState.mediaUri }
                    matchingFile != null
                }
                val b = if (playingBook != null) {
                    val playingBookState = playingBook.copy(currentUri = matchingFile!!.uri, currentPositionMs = mediaState.positionMs).toUiBook()
                    booksState.bookStates.map {
                        if (it.id == playingBookState.id) playingBookState
                        else it
                    }
                } else {
                    booksState.bookStates
                }
                BooksState.Books(b, booksState.selectedIndex, isPlaying = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), BooksState.Initializing)

    private val uiSettings = uiSettingsStore.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    // TODO: rethink whether this should be nullable
    @OptIn(ExperimentalCoroutinesApi::class)
    val batteryState: StateFlow<BatteryState?> = uiSettings
        .map { it.showBatteryIndicator }
        .flatMapLatest { showBatteryIndicator ->
            if (showBatteryIndicator) batteryStateProvider.batteryState
            else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val hideSettingsButton: StateFlow<Boolean> = uiSettings.map {
        it.hideSettingsButton
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val volumeChangeEvent = MutableSharedFlow<Float>(replay = 1, extraBufferCapacity = 1)
        .apply { tryEmit(computeVolume()) }

    init {
        playbackStateFlow
            .filter { it is PlaybackState.MediaState.Playing }
            .onEach { speaker.stop() }
            .launchIn(viewModelScope)
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
        playbackState.shutdown()
        super.onCleared()
    }

    fun play(bookIndex: Int) {
        viewModelScope.launch {
            val book = allUiBooks.first().books.getOrNull(bookIndex)
            if (book != null) {
                playbackState.play(book)
            }
        }
    }

    fun onPageChanged(bookIndex: Int) {
        viewModelScope.launch {
            val book = allUiBooks.first().books.getOrNull(bookIndex)
            speaker.stop()
            if (book != null) {
                if (uiSettings.value.readBookTitles && !isPlaying()) {
                    viewModelScope.launch {
                        speaker.speakAndWait(book.displayName)
                    }
                }
                playbackUiStateRepository.updateLastSelectedBookId(book.id)
            }
        }
    }

    fun volumeUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
        volumeChangeEvent.tryEmit(computeVolume())
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
        volumeChangeEvent.tryEmit(computeVolume())
    }

    private fun computeVolume(): Float {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return volume.toFloat() / max
    }

    private fun isPlaying() = playbackStateFlow.value is PlaybackState.MediaState.Playing

    private fun List<Audiobook>.toUiBook() = map { it.toUiBook() }

    private fun Audiobook.toUiBook() = UiAudiobook(id, displayName, progress)
}
