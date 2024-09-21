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

package com.studio4plus.homerplayer2.samplebooks

import android.net.Uri
import com.studio4plus.homerplayer2.audiobooks.AudiobookFolderManager
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.File

private const val SAMPLES_DIR = "sampleBooks"

@Factory
class SamplesInstaller(
    private val mainScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val downloadSamples: SamplesDownloader,
    private val unpackSamples: SamplesUnpacker,
    private val audiobookFolderManager: AudiobookFolderManager,
) {
    private val currentState = MutableStateFlow<SamplesInstallState>(SamplesInstallState.Idle)
    val state: StateFlow<SamplesInstallState> get() = currentState

    private val errorChannel =
        Channel<SamplesInstallError>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val errorEvent: ReceiveChannel<SamplesInstallError> = errorChannel

    private var installJob: Job? = null

    fun install(cacheDir: File, filesDir: File) {
        if (installJob?.isActive == true) {
            Timber.w("Installation already in progress, aborting")
            installJob?.cancel()
        }

        currentState.value = SamplesInstallState.Downloading
        installJob = mainScope.launch {
            doInstall(cacheDir, filesDir)
            currentState.value = SamplesInstallState.Idle
        }
    }

    fun reset() {
        installJob?.cancel()
    }

    private suspend fun doInstall(cacheDir: File, filesDir: File) {
        val tmpFile = runInterruptible(dispatcherProvider.Io) {
            File.createTempFile("samples", ".zip", cacheDir)
        }
        val destinationFolder = runInterruptible(dispatcherProvider.Io) {
            File(filesDir, SAMPLES_DIR).also {
                if (it.exists())
                    it.deleteRecursively()
                it.mkdirs()
            }
        }
        try {
            downloadSamples(tmpFile)
            currentState.value = SamplesInstallState.Installing
            unpackSamples(tmpFile, destinationFolder)
            audiobookFolderManager.addSamplesFolder(Uri.fromFile(destinationFolder))
        } catch (runtime: RuntimeException) {
            throw runtime
        } catch (e: Exception) {
            Timber.e(e, "Samples installation failed")
            val error = when (currentState.value) {
                SamplesInstallState.Downloading -> SamplesInstallError.Download
                SamplesInstallState.Installing -> SamplesInstallError.Install(e.message ?: "")
                SamplesInstallState.Idle -> throw IllegalStateException()
            }
            errorChannel.trySend(error)
        }
    }
}