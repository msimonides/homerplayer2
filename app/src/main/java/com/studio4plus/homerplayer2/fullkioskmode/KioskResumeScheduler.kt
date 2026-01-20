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

package com.studio4plus.homerplayer2.fullkioskmode

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studio4plus.homerplayer2.base.Constants
import com.studio4plus.homerplayer2.base.intent.CommonIntent
import org.koin.core.annotation.Factory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

private const val KIOSK_RESUME_WORK_NAME = "kiosk-resume"

@Factory
class KioskResumeScheduler(
    private val appContext: Context
) {
    fun schedule(duration: Duration) {
        val workRequest = OneTimeWorkRequestBuilder<KioskResumeWorker>()
            .setInitialDelay(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .build()
        getWorkManager().enqueueUniqueWork(
            KIOSK_RESUME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel() {
        getWorkManager().cancelUniqueWork(KIOSK_RESUME_WORK_NAME)
    }

    private fun getWorkManager(): WorkManager =
        WorkManager.getInstance(appContext)
}

@Factory
class KioskResumeWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val intent = Intent().apply {
            setPackage(Constants.KioskSetupPackage)
            action = CommonIntent.ACTION_KIOSK_RESUME
        }
        appContext.sendBroadcast(intent)
        return Result.success()
    }
}
