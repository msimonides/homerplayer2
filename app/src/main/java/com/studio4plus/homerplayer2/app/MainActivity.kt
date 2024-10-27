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

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.studio4plus.homerplayer2.app.ui.HomerPLayerUi
import io.sentry.Sentry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transformLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val activityViewModel: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: show splash screen until state is Ready.

        enableEdgeToEdge()
        setupLockTask()

        setContent {
            HomerPLayerUi()
        }
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.onResume()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupLockTask() {
        val windowInsetsControllerCompat = WindowInsetsControllerCompat(window, window.decorView)
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityViewModel.lockTask
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            // There's no onLatest, so use transformLatest
            .transformLatest<Boolean, Unit> { isEnabled ->
                if (isEnabled) {
                    windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.statusBars())
                    startLockTaskWithRetry()
                } else {
                    windowInsetsControllerCompat.show(WindowInsetsCompat.Type.statusBars())
                    if (activityManager.isInLockTaskMode()) {
                        // API22 crashes when stopLockTask is being called when not in lock task
                        // mode. The check above can be removed for newer versions.
                        stopLockTask()
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private suspend fun startLockTaskWithRetry() {
        val retries = 4
        repeat(retries + 1) { index ->
            try {
                startLockTask()
                return
            } catch (e: IllegalArgumentException) {
                if (e.message != "Invalid task, not in foreground")
                    throw e
                Timber.e(e, "Error enabling lock task, retrying")
                if (index == retries) {
                    Sentry.captureException(e)
                } else {
                    delay(100)
                }
            }
        }
    }
}


