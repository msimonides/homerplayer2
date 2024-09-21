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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single

@Single
class SamplesInstallController(
    private val appContext: Context,
    mainScope: CoroutineScope
) {
    private val bindService: Flow<SamplesInstallerService.LocalBinder?> = callbackFlow {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                channel.trySend(binder as SamplesInstallerService.LocalBinder)
            }

            override fun onServiceDisconnected(component: ComponentName) {
                channel.trySend(null)
                // Bind again if the service is started again.
                appContext.bindService(Intent(appContext, SamplesInstallerService::class.java), this, 0)
            }
        }
        appContext.bindService(Intent(appContext, SamplesInstallerService::class.java), connection, 0)
        awaitClose { appContext.unbindService(connection) }
    }

    private val boundService: StateFlow<SamplesInstallerService.LocalBinder?> =
        bindService.stateIn(mainScope, SharingStarted.WhileSubscribed(), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val stateFlow = boundService.flatMapLatest { binder ->
        binder?.state ?: flowOf(SamplesInstallState.Idle)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val errorEvent: Flow<SamplesInstallError> = boundService.flatMapLatest { binder ->
        binder?.errorEvent?.receiveAsFlow() ?: flowOf()
    }

    fun start() {
        SamplesInstallerService.startInstall(appContext)
    }

    fun abort() {
        boundService.value?.abort()
    }
}