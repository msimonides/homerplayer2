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

package com.studio4plus.homerplayer2.podcasts.usecases

import android.net.Uri
import androidx.room.withTransaction
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDao
import com.studio4plus.homerplayer2.net.FileDownloader
import com.studio4plus.homerplayer2.podcasts.PodcastsFileStorage
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import org.koin.core.annotation.Single
import timber.log.Timber
import java.io.File
import java.io.IOException

@Single
class DownloadPendingPodcastEpisodes(
    private val db: AppDatabase,
    private val podcastsDao: PodcastsDao,
    private val audiobooksDao: AudiobooksDao,
    private val fileDownloader: FileDownloader,
    private val podcastsStorage: PodcastsFileStorage,
    private val podcastEpisodeName: PodcastEpisodeName,
) {

    suspend operator fun invoke(): Boolean {
        var failure = false
        podcastsDao.getEpisodesForDownload().forEach { episode ->
            try {
                val file = podcastsStorage.getPodcastFile(episode.fileId)
                fileDownloader(file, episode.uri, append = true)
                db.withTransaction {
                    val podcast = podcastsDao.getPodcast(episode.feedUri)
                    if (podcast != null) {
                        addAudiobook(podcast, episode, file)
                        podcastsDao.updateIsDownloaded(episode.uri)
                    } else {
                        Timber.w("No podcast found for uri: ${episode.feedUri}")
                    }
                }
            } catch (e: IOException) {
                failure = true
            }
        }
        return !failure
    }

    private suspend fun addAudiobook(podcast: Podcast, episode: PodcastEpisode, file: File) {
        val displayName = podcastEpisodeName(podcast, episode)
        val bookId = episode.fileId
        val audiobook =
            Audiobook(bookId, displayName = displayName, rootFolderUri = Uri.parse(podcast.feedUri))
        val audiobookFile = AudiobookFile(bookId = bookId, uri = Uri.fromFile(file))
        audiobooksDao.insertAudiobook(audiobook, listOf(audiobookFile))
        Timber.i("Added episode ${episode.uri} to audiobooks '$displayName'")
    }
}