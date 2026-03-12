/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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

package com.studio4plus.homerplayer2.analytics

import android.content.Context
import com.telemetrydeck.sdk.PersistentSignalCache
import com.telemetrydeck.sdk.Signal
import com.telemetrydeck.sdk.SignalCache
import com.telemetrydeck.sdk.TelemetryDeck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

fun createAnalytics(
    mainScope: CoroutineScope,
    delegate: AnalyticsDelegate,
) : Analytics = TelemetryDeckAnalytics(mainScope, delegate)

class TelemetryDeckAnalytics(
    private val mainScope: CoroutineScope,
    delegate: AnalyticsDelegate,
) : Analytics {

    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = mainScope + singleThreadDispatcher
    private val mutex = Mutex() // Needed to synchronize initialization with events.

    override fun initialize(appContext: Context) {
        mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                val cache = withContext(singleThreadDispatcher) {
                    // PersistentSignalCache does I/O in constructor, create it on a background thread.
                    PersistentSignalCache(appContext.cacheDir, null)
                }
                val builder = TelemetryDeck.Builder()
                    .appID(appContext.getString(R.string.telemetry_deck_app))
                    .signalCache(cache)
                // start needs to be called on the main thread to register with LifecycleRegistry.
                TelemetryDeck.start(appContext, builder)
            }
        }
    }

    override fun event(name: String, params: Map<String, String>) {
        // signal writes to disk cache with blocking I/O.
        scope.launch(singleThreadDispatcher) {
            mutex.withLock {
                // delegate.shouldSendImmediately() can be ignored, TelemetryDeck sends data with max
                // 10s delay which should be good enough.
                TelemetryDeck.signal(name, params = params)
            }
        }
    }

    override fun sendErrorEvent(name: String) {
        val errorSignal = "TelemetryDeck.Error.occurred"
        event(errorSignal, params = mapOf("TelemetryDeck.Error.id" to name))
    }

    override fun startDurationEvent(name: String) {
        TelemetryDeck.startDurationSignal(name)
    }

    override fun stopAndSendDurationEvent(name: String) {
        TelemetryDeck.stopAndSendDurationSignal(name)
    }
}
