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

package com.studio4plus.homerplayer2.appreview

import android.app.Activity
import android.app.Application
import com.studio4plus.homerplayer2.app.StoredAppState
import com.studio4plus.homerplayer2.fullkioskmode.UserEnabledFullKioskModeEvents
import com.studio4plus.homerplayer2.lifecycle.CurrentActivity
import com.studio4plus.homerplayer2.testutils.FakeDataStore
import com.studio4plus.homerplayer2.testutils.TestScopeClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.days

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppReviewTriggerTests {

    private lateinit var testScope: TestScope
    private lateinit var events: UserEnabledFullKioskModeEvents
    private lateinit var fakeCurrentActivity: FakeCurrentActivity
    private lateinit var fakeReviewRequester: FakeReviewRequester

    @Before
    fun setup() {
        testScope = TestScope()
        testScope.advanceTimeBy(1_000) // 0 is used as an unset timestamp sentinel.

        events = UserEnabledFullKioskModeEvents()
        fakeReviewRequester = FakeReviewRequester()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        fakeCurrentActivity = FakeCurrentActivity(activity)
    }

    @Test
    fun `requests review after explicit user enable when app age and cooldown are satisfied`() =
        testScope.runTest {
            val now = currentTime
            val appStateStore = setupTrigger(
                firstRunTimestampMs = now - 8.days.inWholeMilliseconds,
                reviewLastRequestedTimestampMs = 0L,
            )

            events.emit()
            advanceUntilIdle()

            assertEquals(1, fakeReviewRequester.requestCount)
            assertTrue(appStateStore.data.first().reviewLastRequestedTimestampMs > 0L)
        }

    @Test
    fun `does not request review when app is younger than required`() = testScope.runTest {
        val now = currentTime
        setupTrigger(
            firstRunTimestampMs = now - 2.days.inWholeMilliseconds,
            reviewLastRequestedTimestampMs = 0L,
        )

        events.emit()
        advanceUntilIdle()

        assertEquals(0, fakeReviewRequester.requestCount)
    }

    @Test
    fun `does not request review during cooldown`() = testScope.runTest {
        val now = currentTime
        setupTrigger(
            firstRunTimestampMs = now - 120.days.inWholeMilliseconds,
            reviewLastRequestedTimestampMs = now - 14.days.inWholeMilliseconds,
        )

        events.emit()
        advanceUntilIdle()

        assertEquals(0, fakeReviewRequester.requestCount)
    }

    private fun TestScope.setupTrigger(
        firstRunTimestampMs: Long,
        reviewLastRequestedTimestampMs: Long,
    ): FakeDataStore<StoredAppState> {
        val appStateStore = FakeDataStore(
            StoredAppState(
                firstRunTimestampMs = firstRunTimestampMs,
                reviewLastRequestedTimestampMs = reviewLastRequestedTimestampMs,
            )
        )
        AppReviewTrigger(
            mainScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            appStateStore = appStateStore,
            userEnabledFullKioskModeEvents = events,
            currentActivity = fakeCurrentActivity,
            reviewRequester = fakeReviewRequester,
            clock = TestScopeClock(testScope),
        )
        return appStateStore
    }

    private class FakeCurrentActivity(initialActivity: Activity?) : CurrentActivity {
        private val state = MutableStateFlow(initialActivity)

        override fun invoke(): Flow<Activity?> = state
    }

    private class FakeReviewRequester : ReviewRequester {
        var requestCount: Int = 0
            private set

        override suspend fun requestReview(activity: Activity) {
            requestCount += 1
        }
    }
}
