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
import com.telemetrydeck.sdk.TelemetryDeck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun createAnalytics(
    mainScope: CoroutineScope,
    delegate: AnalyticsDelegate,
) : Analytics = TelemetryDeckAnalytics(mainScope, delegate)

class TelemetryDeckAnalytics(
    private val mainScope: CoroutineScope,
    private val delegate: AnalyticsDelegate,
) : Analytics {

    override fun initialize(appContext: Context) {
        val builder = TelemetryDeck.Builder()
            .appID(appContext.getString(R.string.telemetry_deck_app))
            .showDebugLogs(true)
        TelemetryDeck.start(appContext, builder)
    }

    override fun event(name: String, params: Map<String, String>) {
        if (delegate.shouldSendImmediately()) {
            mainScope.launch {
                TelemetryDeck.send(name, additionalPayload = params)
            }
        } else {
            TelemetryDeck.signal(name, params = params)
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