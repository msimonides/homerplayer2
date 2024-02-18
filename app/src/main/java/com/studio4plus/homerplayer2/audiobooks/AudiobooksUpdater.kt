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

package com.studio4plus.homerplayer2.audiobooks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.koin.core.annotation.Single

@Single
class AudiobooksUpdater(
    mainScope: CoroutineScope,
    audiobookFoldersDao: AudiobookFoldersDao,
    private val audiobooksDao: AudiobooksDao,
    private val scanner: Scanner
) {
    private val triggerFlow: MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> get() = _isScanning

    init {
        combine(
            triggerFlow,
            audiobookFoldersDao.getAll()
        ) { _, audiobooksFolders ->
            advertiseBusy(_isScanning) {
                scan(audiobooksFolders)
            }
        }.launchIn(mainScope)
    }

    fun trigger() {
        triggerFlow.tryEmit(Unit)
    }

    private suspend fun scan(folders: List<AudiobooksFolder>) {
        val scannedItems = scanner.scan(folders.map { it.uri }).map { item ->
            Pair(item.audiobook, item.uris.map { AudiobookFile(it, item.audiobook.id) })
        }
        audiobooksDao.replaceAll(scannedItems.map { it.first }, scannedItems.flatMap { it.second })
    }
}

private inline fun advertiseBusy(busyState: MutableStateFlow<Boolean>, action: () -> Unit) {
    try {
        busyState.value = true
        action()
    } finally {
        busyState.value = false
    }
}
