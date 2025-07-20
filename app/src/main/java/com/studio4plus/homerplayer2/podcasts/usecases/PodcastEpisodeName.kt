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

import android.content.Context
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.LocaleProvider
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import org.koin.core.annotation.Factory
import java.text.SimpleDateFormat

@Factory
class PodcastEpisodeName(
    private val appContext: Context,
    private val localeProvider: LocaleProvider,
) {

    operator fun invoke(podcast: Podcast, episode: PodcastEpisode): String = with (podcast) {
        when {
            includePodcastTitle && includeEpisodeDate && includeEpisodeTitle ->
                appContext.getString(
                    R.string.podcast_episode_name_title_date_episode,
                    podcast.displayName(),
                    formatEpisodeDate(episode),
                    episode.title
                )
            includePodcastTitle && includeEpisodeTitle ->
                appContext.getString(
                    R.string.podcast_episode_name_title_episode, podcast.displayName(), episode.title
                )
            includePodcastTitle && includeEpisodeDate ->
                appContext.getString(
                    R.string.podcast_episode_name_title_date, podcast.displayName(), formatEpisodeDate(episode)
                )
            includeEpisodeTitle ->
                episode.title
            else ->
                throw IllegalStateException(
                    "Unsupported episode name combination: podcast=$includePodcastTitle; date=$includeEpisodeDate; episode=$includeEpisodeTitle"
                )
        }
    }

    private fun formatEpisodeDate(episode: PodcastEpisode): String =
        // Already downloaded podcast episodes can have a null publication time,
        // newly downloaded ones will get a date.
        episode.publicationTime?.let { date ->
            // SimpleDateFormat has correct localization of MMMM as opposed to desugared
            // java.time.DateTimeFormatter.
            // Not cached because it's not thread safe, this code is not being called that often.
            SimpleDateFormat(
                appContext.getString(R.string.podcast_episode_name_date_format),
                localeProvider()
            ).format(date.toEpochMilli())
        } ?: ""
}

private fun Podcast.displayName() = titleOverride ?: title