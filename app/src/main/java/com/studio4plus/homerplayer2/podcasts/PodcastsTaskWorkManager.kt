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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import com.studio4plus.homerplayer2.podcasts.usecases.DeleteStalePodcastFiles
import com.studio4plus.homerplayer2.podcasts.usecases.DownloadPendingPodcastEpisodes
import com.studio4plus.homerplayer2.podcasts.usecases.DownloadPodcastFeed
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastFromFeed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val REFRESH_WORK_ID = "podcasts refresh"

@Factory
class PodcastTaskWorkManager(
    private val appContext: Context
) : PodcastsTaskScheduler {

    override fun enablePeriodicUpdate() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<PodcastsRefreshWork>(
            repeatInterval = 12, repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 3, flexTimeIntervalUnit = TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(REFRESH_WORK_ID, ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    override fun disablePeriodicUpdate() {
        WorkManager.getInstance(appContext).cancelUniqueWork(REFRESH_WORK_ID)
    }

    override fun runUpdate() {
        val workRequest = OneTimeWorkRequestBuilder<PodcastsRefreshWork>().build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(REFRESH_WORK_ID, ExistingWorkPolicy.REPLACE, workRequest)
    }
}

class PodcastsRefreshWork(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val feedDownload: DownloadPodcastFeed by inject()
    private val feedUpdater: UpdatePodcastFromFeed by inject()
    private val episodeDownloader: DownloadPendingPodcastEpisodes by inject()
    private val podcastsDao: PodcastsDao by inject()
    private val deleteStalePodcastFiles: DeleteStalePodcastFiles by inject()

    override suspend fun doWork(): Result {
        deleteStalePodcastFiles()

        val podcasts = podcastsDao.getPodcasts()
        var isSuccess = true
        podcasts.forEach { podcast ->
            val download = feedDownload(podcast.feedUri)
            if (download is DownloadPodcastFeed.Result.Success) {
                feedUpdater(podcast, download.feed)
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

    companion object {
        private val episodeDownloaderLock = Mutex()
    }
}