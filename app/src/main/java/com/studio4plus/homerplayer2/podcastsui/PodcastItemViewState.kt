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

package com.studio4plus.homerplayer2.podcastsui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.studio4plus.homerplayer2.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PodcastItemViewState(
    val feedUri: String,
    val displayName: String,
    val latestEpisodeDate: LocalDate?,
)

@Composable
fun PodcastItemViewState.subLabel(dateFormatter: DateTimeFormatter) = when (latestEpisodeDate) {
    null -> stringResource(R.string.podcast_info_no_episodes_found)
    else -> stringResource(R.string.podcast_info_latest_episode_date, dateFormatter.format(latestEpisodeDate))
}

@Composable
fun BoxScope.PodcastBadgeContent() {
    Icon(
        Icons.Default.Podcasts,
        contentDescription = null,
        modifier = Modifier.align(Alignment.Center)
    )
}
