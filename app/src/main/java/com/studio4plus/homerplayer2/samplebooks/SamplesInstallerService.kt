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

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class SamplesInstallerService : LifecycleService() {

    class LocalBinder(private val samplesInstaller: SamplesInstaller) : Binder() {
        val state: StateFlow<SamplesInstallState> = samplesInstaller.state
        val errorEvent: ReceiveChannel<SamplesInstallError> = samplesInstaller.errorEvent

        fun abort() {
            samplesInstaller.reset()
        }
    }

    private val samplesInstaller: SamplesInstaller by inject()
    private val binder = LocalBinder(samplesInstaller)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("starting with action ${intent?.action}")
        return when (intent?.action) {
            ACTION_INSTALL -> {
                samplesInstaller.install(cacheDir, filesDir)
                lifecycleScope.launch {
                    samplesInstaller.state.first { it == SamplesInstallState.Idle }
                    stopSelf()
                }
                START_NOT_STICKY
            }

            else -> super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    companion object {
        private const val ACTION_INSTALL = "Install"

        fun startInstall(context: Context) {
            val intent = Intent(context, SamplesInstallerService::class.java).apply {
                action = ACTION_INSTALL
            }
            context.startService(intent)
        }
    }
}

