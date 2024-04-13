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

package com.studio4plus.homerplayer2.fullkioskmode

import app.cash.turbine.test
import com.studio4plus.homerplayer2.settingsdata.FullKioskModeSetting
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.testutils.FakeDataStore
import com.studio4plus.homerplayer2.testutils.FakeIsInForeground
import com.studio4plus.homerplayer2.testutils.TestClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class IsFullKioskModeEnabledTests {

    private lateinit var isInForeground: FakeIsInForeground
    private lateinit var uiSettingsStore: FakeDataStore<UiSettings>
    private lateinit var testScope: TestScope

    private lateinit var isFullKioskEnabled: IsFullKioskEnabled

    @Before
    fun setup() {
        testScope = TestScope()
        testScope.advanceTimeBy(1_000) // 0 is a special value.

        isInForeground = FakeIsInForeground()
        uiSettingsStore = FakeDataStore(UiSettings())
        isFullKioskEnabled = IsFullKioskEnabled(
            uiSettings = uiSettingsStore,
            isFullKioskAvailable = { true },
            isInForeground = isInForeground,
            clock = TestClock(testScope)
        )
    }

    @Test
    fun `initial value is emitted`() = testScope.runTest {
        val value = isFullKioskEnabled().first()
        assertEquals(IsFullKioskEnabled.Disabled, value)
    }

    @Test
    fun `setting changes while in background are emitted`() = testScope.runTest {
        isFullKioskEnabled().test {
            assertEquals(IsFullKioskEnabled.Disabled, awaitItem())
            uiSettingsStore.updateData { it.copy(fullKioskModeEnableTimestamp = FullKioskModeSetting.ENABLED) }
            assertEquals(IsFullKioskEnabled.Enabled, awaitItem())
        }
    }

    @Test
    fun `setting change in background enabling in future emitted when elapses in foreground`() =
        testScope.runTest {
            val elapseTime = currentTime + 1_000
            uiSettingsStore.updateData { it.copy(fullKioskModeEnableTimestamp = elapseTime) }
            advanceTimeBy(500)
            isFullKioskEnabled().test {
                isInForeground.value = true
                assertEquals(
                    IsFullKioskEnabled.DisabledUntil(elapseTime.toLocalDateTime()),
                    awaitItem()
                )
                advanceTimeBy(500)
                assertEquals(IsFullKioskEnabled.Enabled, awaitItem())
            }
        }

    @Test
    fun `setting change in background enabling in future emitted after elasped whne going foregrouns`() =
        testScope.runTest {
            val elapseTime = currentTime + 1_000
            uiSettingsStore.updateData { it.copy(fullKioskModeEnableTimestamp = elapseTime) }
            isFullKioskEnabled().test {
                assertEquals(
                    IsFullKioskEnabled.DisabledUntil(elapseTime.toLocalDateTime()),
                    awaitItem()
                )
                advanceTimeBy(2_000)
                ensureAllEventsConsumed()
                isInForeground.value = true
                assertEquals(IsFullKioskEnabled.Enabled, awaitItem())
            }
        }

    @Test
    fun `future enable time updated before elapsing is emitted only for new value`() =
        testScope.runTest {
            val elapseTime1 = currentTime + 1_000
            val elapseTime2 = currentTime + 2_000
            uiSettingsStore.updateData { it.copy(fullKioskModeEnableTimestamp = elapseTime1) }
            isInForeground.value = true

            isFullKioskEnabled().test {
                assertEquals(
                    IsFullKioskEnabled.DisabledUntil(elapseTime1.toLocalDateTime()),
                    awaitItem()
                )
                advanceTimeBy(500)
                uiSettingsStore.updateData { it.copy(fullKioskModeEnableTimestamp = elapseTime2) }
                assertEquals(
                    IsFullKioskEnabled.DisabledUntil(elapseTime2.toLocalDateTime()),
                    awaitItem()
                )
                advanceTimeBy(1_000)
                ensureAllEventsConsumed()
                advanceTimeBy(500)
                assertEquals(IsFullKioskEnabled.Enabled, awaitItem())
            }
        }

    private fun Long.toLocalDateTime() =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), TimeZone.getDefault().toZoneId())
}