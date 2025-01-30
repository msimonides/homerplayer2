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

package com.studio4plus.homerplayer2.player.service

import android.net.Uri
import androidx.media3.common.Player
import com.studio4plus.homerplayer2.audiobookfolders.AudiobooksFolderSettings
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.player.PlaybackUiStateRepository
import com.studio4plus.homerplayer2.utils.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@OptIn(ExperimentalCoroutinesApi::class)
@Factory
class PlayPositionUpdater(
    private val mainScope: CoroutineScope,
    private val audiobooksDao: AudiobooksDao,
    private val playbackUiStateRepository: PlaybackUiStateRepository,
) : Player.Listener {

    // TODO: handle changes via notification while paused.

    private data class Position(val fileUri: Uri, val position: Long)

    private sealed interface State {
        data class Playing(val player: Player) : State
        data class Stopped(val lastPosition: Position?) : State
    }

    private val stateFlow = MutableStateFlow<State>(State.Stopped(null))

    init {
        stateFlow.flatMapLatest { state ->
            when (state) {
                is State.Playing -> {
                    // Update selected book to always match the most recently played one.
                    updateLastSelectedBook(state.player)
                    tickerFlow(5_000L)
                        .mapNotNull {
                            val currentFileUri = state.player.currentMediaItem?.localConfiguration?.uri
                            currentFileUri?.let {
                                Position(it, state.player.currentPosition)
                            }
                        }
                }
                is State.Stopped ->
                    if (state.lastPosition != null) {
                        flowOf(state.lastPosition)
                    } else {
                        emptyFlow()
                    }
            }
        }.onEach { position ->
            mainScope.launch {
                audiobooksDao.updatePlayPosition(position.fileUri, position.position)
            }
        }.launchIn(mainScope)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        // onEvents is a more cumbersome API but it gets the Player.
        if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
            if (player.isPlaying) {
                stateFlow.value = State.Playing(player)
            } else {
                val currentFileUri = player.currentMediaItem?.localConfiguration?.uri
                val position = currentFileUri?.let { Position(it, player.currentPosition) }
                stateFlow.value = State.Stopped(lastPosition = position)

                // Rewinding triggers another event.
                rewindToBeginningIfNeeded(player, currentFileUri)
            }
        }
    }

    private fun updateLastSelectedBook(player: Player) {
        val currentFileUri = player.currentMediaItem?.localConfiguration?.uri ?: return
        mainScope.launch {
            val currentFile = audiobooksDao.getAudiobookFile(currentFileUri)
            if (currentFile != null) {
                playbackUiStateRepository.updateLastSelectedBookId(currentFile.bookId)
            }
        }
    }

    private fun rewindToBeginningIfNeeded(player: Player, currentFileUri: Uri?) {
        if (player.playbackState == Player.STATE_ENDED && currentFileUri != null) {
            mainScope.launch {
                // Note: defaults will also be used for podcasts. Better podcast handling
                // may be needed in the future.
                val folderSettings = audiobooksDao.getSettingsByFile(currentFileUri)
                    ?: AudiobooksFolderSettings.defaults
                if (folderSettings.rewindOnEnd) {
                    player.seekTo(0, 0L)
                    player.stop()
                    // This will trigger another update and save the position at start.
                }
            }
        }
    }
}
