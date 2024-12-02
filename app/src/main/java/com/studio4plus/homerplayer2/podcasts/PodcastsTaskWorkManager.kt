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

package com.studio4plus.homerplayer2.podcasts

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import com.studio4plus.homerplayer2.podcasts.usecases.DeleteStalePodcastFiles
import com.studio4plus.homerplayer2.podcasts.usecases.DownloadPendingPodcastEpisodes
import com.studio4plus.homerplayer2.podcasts.usecases.DownloadPodcastFeed
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastFromFeed
import com.studio4plus.homerplayer2.settingsdata.NetworkType
import io.sentry.Sentry
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val REFRESH_PERIODIC_WORK_ID = "podcasts periodic refresh"
private const val REFRESH_NOW_WORK_ID = "podcasts refresh once"

@Factory
class PodcastTaskWorkManager(
    private val appContext: Context
) : PodcastsTaskScheduler {

    override suspend fun enablePeriodicUpdate(networkType: NetworkType) {
        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(
                REFRESH_PERIODIC_WORK_ID,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequestBuilder(networkType).build()
            )
    }

    override fun disablePeriodicUpdate() {
        WorkManager.getInstance(appContext).cancelUniqueWork(REFRESH_PERIODIC_WORK_ID)
    }

    override suspend fun updateNetworkType(networkType: NetworkType) {
        val workManager = WorkManager.getInstance(appContext)

        // Once Worker updates are fixed to cancel running work when updated constraints are not
        // satisfied this code should no longer be needed
        // https://issuetracker.google.com/issues/380600742
        suspend fun <B : WorkRequest.Builder<B, *>> updateWorkerConstraints(
            workInfo: WorkInfo?,
            workRequestFactory: () -> WorkRequest.Builder<B, *>,
            cancelAndEnqueue: (WorkRequest) -> Unit,
        ) {
            if (workInfo != null) {
                val workRequest = workRequestFactory().setId(workInfo.id).build()
                val updateResult = workManager.updateWork(workRequest).await()
                val oldNetworkType = workInfo.constraints.requiredNetworkType
                val needsAbort = updateResult == WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN
                        && networkType == NetworkType.Unmetered
                        && oldNetworkType != networkType.toWorkRequest()
                if (needsAbort) {
                    cancelAndEnqueue(workRequest)
                }
            }
        }

        updateWorkerConstraints(
            workManager.getFirstWorkInfo(REFRESH_PERIODIC_WORK_ID),
            workRequestFactory = { periodicWorkRequestBuilder(networkType) },
            cancelAndEnqueue = { workRequest ->
                workManager.enqueueUniquePeriodicWork(
                    REFRESH_PERIODIC_WORK_ID,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest as PeriodicWorkRequest
                )
            }
        )

        updateWorkerConstraints(
            workManager.getFirstWorkInfo(REFRESH_NOW_WORK_ID),
            workRequestFactory = { oneTimeWorkRequestBuilder(networkType) },
            cancelAndEnqueue = { runUpdate(networkType) }
        )
    }

    override fun runUpdate(networkType: NetworkType) {
        val workRequest = oneTimeWorkRequestBuilder(networkType).build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(REFRESH_NOW_WORK_ID, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun periodicWorkRequestBuilder(networkType: NetworkType): PeriodicWorkRequest.Builder {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(networkType.toWorkRequest())
            .build()
        return PeriodicWorkRequestBuilder<PodcastsRefreshWork>(
            repeatInterval = 8, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 4, flexTimeIntervalUnit = TimeUnit.HOURS
        ).setConstraints(constraints)

    }

    private fun oneTimeWorkRequestBuilder(networkType: NetworkType): OneTimeWorkRequest.Builder {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType.toWorkRequest())
            .build()
        return OneTimeWorkRequestBuilder<PodcastsRefreshWork>()
            .setConstraints(constraints)
    }

    private suspend fun WorkManager.getFirstWorkInfo(uniqueName: String): WorkInfo? =
        getWorkInfosForUniqueWork(uniqueName).await().firstOrNull()
}

class PodcastsRefreshWork(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val feedDownload: DownloadPodcastFeed by inject()
    private val feedUpdater: UpdatePodcastFromFeed by inject()
    private val deleteStalePodcastFiles: DeleteStalePodcastFiles by inject()
    private val episodeDownloader: DownloadPendingPodcastEpisodes by inject()
    private val podcastsDao: PodcastsDao by inject()
    private val wifiManager: WifiManager by inject()

    override suspend fun doWork(): Result {
        Timber.i("Starting podcasts update...")
        val wifiLock = createWifiLock()
        return try {
            wifiLock.acquire()
            deleteStalePodcastFiles()

            val result = updateAndDownloadPodcasts()
            Timber.i("Podcast update result: $result")
            result
        } catch (e: Throwable) {
            Timber.w(e, "Error while updating podcasts")
            Sentry.captureException(e)
            Result.failure()
        } finally {
            wifiLock.release()
            Timber.i("Podcasts update finished")
        }
    }

    private suspend fun updateAndDownloadPodcasts(): Result {
        val podcasts = podcastsDao.getPodcasts()
        var isSuccess = true
        Timber.i("Updating ${podcasts.size} feeds")
        podcasts.forEach { podcast ->
            val download = feedDownload(podcast.feedUri)
            if (download is DownloadPodcastFeed.Result.Success) {
                isSuccess = feedUpdater(podcast, download.feed) && isSuccess
            } else {
                isSuccess = false
            }
        }
        val downloadedAll = episodeDownloaderLock.withLock {
            // TODO: OkHttp's cancellation might not be so great in which case even with this lock
            //  some file corruption might occur.
            episodeDownloader()
        }

        isSuccess = isSuccess && downloadedAll
        return if (isSuccess) Result.success() else Result.failure()
    }

    private fun createWifiLock(): WifiManager.WifiLock {
        val tag = "podcast refresh"
        // Don't care about low latency, just keep the WiFi on until everything is downloaded.
        return if (Build.VERSION.SDK_INT < 29) {
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, tag)
        } else {
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, tag)
        }
    }

    companion object {
        private val episodeDownloaderLock = Mutex()
    }
}

private fun NetworkType.toWorkRequest() = when(this) {
    NetworkType.Any -> androidx.work.NetworkType.CONNECTED
    NetworkType.Unmetered -> androidx.work.NetworkType.UNMETERED
}
