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

import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.analytics.Analytics
import com.studio4plus.homerplayer2.app.DATASTORE_APP_STATE
import com.studio4plus.homerplayer2.app.StoredAppState
import com.studio4plus.homerplayer2.lifecycle.CurrentActivity
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.days

private val REVIEW_COOLDOWN_MS = 90.days.inWholeMilliseconds

@Single(createdAtStart = true)
class AppReviewTrigger(
    mainScope: CoroutineScope,
    @Named(DATASTORE_APP_STATE) private val appStateStore: DataStore<StoredAppState>,
    appReviewOpportunity: AppReviewOpportunity,
    private val currentActivity: CurrentActivity,
    private val reviewRequester: ReviewRequester,
    private val clock: Clock,
    private val analytics: Lazy<Analytics>,
) {
    init {
        appReviewOpportunity.events()
            .onEach { maybeRequestReview(it) }
            .launchIn(mainScope)
    }

    private suspend fun maybeRequestReview(reason: AppReviewOpportunity.Reason) {
        val now = clock.wallTime()
        val appState = appStateStore.data.first()
        if (!appState.isEligibleForReview(now, reason)) return

        val activity = currentActivity().first() ?: return

        appStateStore.updateData { it.copy(reviewLastRequestedTimestampMs = now) }

        analytics.value.event("InAppReview.Request", mapOf("reason" to reason.name))
        reviewRequester.requestReview(activity)
    }

    private fun StoredAppState.isEligibleForReview(
        nowMs: Long,
        reason: AppReviewOpportunity.Reason,
    ): Boolean {
        if (firstRunTimestampMs == StoredAppState.UNSET_TIMESTAMP_MS) return false
        if (nowMs - firstRunTimestampMs < minAppAge(reason)) return false
        val needsCooldown =
            reviewLastRequestedTimestampMs != StoredAppState.UNSET_TIMESTAMP_MS &&
                nowMs - reviewLastRequestedTimestampMs < REVIEW_COOLDOWN_MS
        if (needsCooldown) return false

        return true
    }

    private fun minAppAge(reason: AppReviewOpportunity.Reason): Long = when (reason) {
        AppReviewOpportunity.Reason.KIOSK_ENABLED -> 0
        AppReviewOpportunity.Reason.OPENED_SETTINGS -> 7.days.inWholeMilliseconds
    }
}
