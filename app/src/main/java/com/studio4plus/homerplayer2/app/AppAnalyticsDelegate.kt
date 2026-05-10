/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.app

import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.analytics.AnalyticsDelegate
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskAvailable
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KProperty

private const val SEND_IMMEDIATELY_AGE_MINUTES = 60L
private val UNSET = Instant.ofEpochMilli(StoredAppState.UNSET_TIMESTAMP_MS)

@Single
class AppAnalyticsDelegate(
    mainScope: CoroutineScope,
    @Named(DATASTORE_APP_STATE) appState: DataStore<StoredAppState>,
    private val clock: Clock,
    fullKioskAvailable: IsFullKioskAvailable,
) : AnalyticsDelegate {
    private var firstLaunchTimestamp: Instant = UNSET
    private val isFullKioskAvailable by CachingDelegate(
        clock,
        fullKioskAvailable::invoke,
        Duration.ofHours(1)
    )

    init {
        mainScope.launch {
            firstLaunchTimestamp = Instant.ofEpochMilli(appState.data.first().firstRunTimestampMs)
        }
    }

    override fun shouldSendImmediately(): Boolean {
        val base = firstLaunchTimestamp
        return base == UNSET || base + Duration.ofMinutes(SEND_IMMEDIATELY_AGE_MINUTES) > clock.wallInstant()
    }

    override fun defaultParams(): Map<String, String> =
        mapOf("Settings.FullKioskAvailable" to isFullKioskAvailable.toString())
}

private class CachingDelegate<T : Any>(
    private val clock: Clock,
    private val provider: () -> T,
    private val cacheDuration: Duration,
) {
    private lateinit var value: T
    private var lastUpdatedMs: Long = 0

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val now = clock.elapsedRealTime()
        if (lastUpdatedMs == 0L || lastUpdatedMs + cacheDuration.toMillis() < now) {
            value = provider()
            lastUpdatedMs = now
        }
        return value
    }
}