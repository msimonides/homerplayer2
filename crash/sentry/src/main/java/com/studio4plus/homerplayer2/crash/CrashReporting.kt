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

package com.studio4plus.homerplayer2.crash

import android.content.Context
import android.os.Build
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import java.util.concurrent.ConcurrentHashMap

object CrashReporting : CrashReportingInterface {

    private val reportedAlready = ConcurrentHashMap<String, Unit>()

    override fun init(appContext: Context) {
        SentryAndroid.init(appContext) { options ->
            options.dsn = appContext.getString(R.string.sentry_dsn)
            options.isAnrEnabled = Build.VERSION.SDK_INT >= 30
            options.isEnableAppLifecycleBreadcrumbs = true
            options.isEnableAppComponentBreadcrumbs = true

            options.isEnableUserInteractionBreadcrumbs = false
            options.isEnableActivityLifecycleBreadcrumbs = false
            options.isEnableNetworkEventBreadcrumbs = false

            val timber = SentryTimberIntegration(
                minEventLevel = SentryLevel.FATAL,
                minBreadcrumbLevel = SentryLevel.INFO,
            )
            options.addIntegration(timber)
        }
    }

    override fun captureException(e: Throwable) {
        Sentry.captureException(e)
    }

    override fun captureExceptionOnce(key: String, builder: () -> Throwable) {
        if (!reportedAlready.containsKey(key)) {
            reportedAlready[key] = Unit
            Sentry.captureException(builder())
        }
    }

    override fun setContext(key: String, value: String) {
        Sentry.setExtra(key, value)
    }

    override fun removeContext(key: String) {
        Sentry.removeExtra(key)
    }
}