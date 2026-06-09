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
import com.studio4plus.homerplayer2.app.ui.HomerPlayerUiVM
import com.studio4plus.homerplayer2.testutils.FakeCurrentActivity
import com.studio4plus.homerplayer2.settingsdata.UiSettings
import com.studio4plus.homerplayer2.testutils.FakeDataStore
import com.studio4plus.homerplayer2.testutils.FakeReviewRequester
import com.studio4plus.homerplayer2.testutils.TestAnalytics
import com.studio4plus.homerplayer2.testutils.TestScopeClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AppReviewOpenedSettingsIntegrationTests {

    private lateinit var testScope: TestScope
    private lateinit var opportunities: AppReviewOpportunity
    private lateinit var fakeCurrentActivity: FakeCurrentActivity
    private lateinit var fakeReviewRequester: FakeReviewRequester
    private lateinit var analytics: TestAnalytics

    @Before
    fun setup() {
        testScope = TestScope()
        testScope.advanceTimeBy(100.days) // 0 is used as an unset timestamp sentinel.

        opportunities = AppReviewOpportunity()
        analytics = TestAnalytics()
        fakeReviewRequester = FakeReviewRequester()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        fakeCurrentActivity = FakeCurrentActivity(activity)
    }

    @Test
    fun `opening settings from ui emits opportunity that reaches review trigger`() =
        testScope.runTest {
            val now = currentTime
            val appStateStore = FakeDataStore(
                StoredAppState(
                    firstRunTimestampMs = now - 8.days.inWholeMilliseconds,
                    reviewLastRequestedTimestampMs = StoredAppState.UNSET_TIMESTAMP_MS,
                )
            )
            val uiSettingsStore = FakeDataStore(UiSettings())

            AppReviewTrigger(
                mainScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
                appStateStore = appStateStore,
                appReviewOpportunity = opportunities,
                currentActivity = fakeCurrentActivity,
                reviewRequester = fakeReviewRequester,
                clock = TestScopeClock(this),
                analytics = lazyOf(analytics),
            )
            val viewModel = HomerPlayerUiVM(appStateStore, uiSettingsStore, opportunities)

            viewModel.onOpenedSettings()
            advanceUntilIdle()

            assertEquals(1, fakeReviewRequester.requestCount)
            assertEquals(
                listOf(TestAnalytics.RecordedEvent.Event("InAppReview.Request", mapOf("reason" to "OPENED_SETTINGS"))),
                analytics.recordedEvents,
            )
            assertTrue(appStateStore.data.first().reviewLastRequestedTimestampMs > 0L)
        }

}



