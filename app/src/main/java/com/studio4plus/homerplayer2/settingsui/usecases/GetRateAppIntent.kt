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

package com.studio4plus.homerplayer2.settingsui.usecases

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.app.DATASTORE_APP_STATE
import com.studio4plus.homerplayer2.app.StoredAppState
import com.studio4plus.homerplayer2.app.StoredAppState.Companion.UNSET_TIMESTAMP_MS
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private val RATING_AVAILABILITY_DELAY: Duration = 5.days

@Factory
class GetRateAppIntent(
    private val appContext: Context,
    private val clock: Clock,
    @Named(DATASTORE_APP_STATE) private val appState: DataStore<StoredAppState>,
) {

    suspend operator fun invoke(): Intent? {
        val firstRunTimestamp = appState.data.first().firstRunTimestampMs
        return if (
            firstRunTimestamp != UNSET_TIMESTAMP_MS &&
                firstRunTimestamp + RATING_AVAILABILITY_DELAY.inWholeMilliseconds < clock.wallTime()
        ) {
            val intent = playStorePageIntent()
            intent.takeIf { it.resolveActivity(appContext.packageManager) != null }
        } else {
            null
        }
    }

    private fun playStorePageIntent() =
        Intent(Intent.ACTION_VIEW).apply {
            val packageName = appContext.packageName
            data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            // https://developer.android.com/distribute/marketing-tools/linking-to-google-play#android-app
            setPackage("com.android.vending")
        }
}
