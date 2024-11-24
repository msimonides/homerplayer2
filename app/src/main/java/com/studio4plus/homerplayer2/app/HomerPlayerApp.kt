/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

import android.app.Application
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.logging.FileLoggerTreeProvider
import io.sentry.android.core.SentryAndroid
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import timber.log.Timber

class HomerPlayerApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    private val okHttpClient by inject<OkHttpClient>()

    override fun onCreate() {
        super.onCreate()
        initCrashReporting()

        startKoin {
            androidContext(this@HomerPlayerApp)
            modules(AppModule().module)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val fileLoggerProvider: FileLoggerTreeProvider by inject()
        Timber.plant(fileLoggerProvider())

        val appIsInForeground: AppIsInForeground by inject()
        registerActivityLifecycleCallbacks(appIsInForeground)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient }
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(8 * 1024 * 1024)
                    .build()
            }
            .diskCachePolicy(CachePolicy.DISABLED) // It's only for podcast search.
            .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    // Try to share Sentry configuration between app and kiosksetup.
    private fun initCrashReporting() {
        if (!BuildConfig.DEBUG) {
            SentryAndroid.init(this) { options ->
                options.dsn = getString(R.string.sentry_dsn)
                options.isAnrEnabled = false
                options.isEnableAppLifecycleBreadcrumbs = true
                options.isEnableAppComponentBreadcrumbs = true

                options.isEnableUserInteractionBreadcrumbs = false
                options.isEnableActivityLifecycleBreadcrumbs = false
                options.isEnableNetworkEventBreadcrumbs = false
            }
        }
    }
}