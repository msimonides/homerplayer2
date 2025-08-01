/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

import androidx.annotation.VisibleForTesting
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssItem
import com.studio4plus.homerplayer2.podcasts.MAX_PODCAST_EPISODE_COUNT
import io.sentry.Sentry
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField

data class PodcastFeedEpisode(
    val rssItem: RssItem,
    val publicationTime: Instant?
)

data class PodcastFeed(
    val title: String,
    val latestEpisodes: List<PodcastFeedEpisode> // Ordered newest to oldest.
)

@Factory
class ParsePodcastFeed(
    private val rssParser: RssParser,
) {

    suspend operator fun invoke(text: String, uri: String): PodcastFeed? =
        try {
            val feed = rssParser.parse(text)
            if (feed.title.isNullOrBlank() || feed.items.isEmpty()) {
                Timber.w("Error validating $uri: no title or no episodes")
                null
            } else {
                val latestEpisodes = feed.items
                    .mapNotNull {
                        when {
                            it.audio != null -> PodcastFeedEpisode(it, it.pubDate?.parseRssDate())
                            else -> null
                        }
                    }
                    .sortedByDescending { it.publicationTime }
                    .take(MAX_PODCAST_EPISODE_COUNT)
                PodcastFeed(requireNotNull(feed.title), latestEpisodes)
            }
        } catch (illegalArgument: IllegalArgumentException) {
            // RssParser throws IllegalArgumentException when input is not a valid XML.
            Timber.i(illegalArgument, "Not valid XML $uri: ${text.take(100)}")
            null
        } catch (parseException: RssParsingException) {
            Timber.w(parseException, "Error parsing $uri: ${text.take(100)}")
            null
        }

    companion object {
        private val dow = HashMap<Long, String>().apply {
            put(1L, "Mon")
            put(2L, "Tue")
            put(3L, "Wed")
            put(4L, "Thu")
            put(5L, "Fri")
            put(6L, "Sat")
            put(7L, "Sun")
        }
        private val moy = HashMap<Long, String>().apply {
            put(1L, "Jan")
            put(2L, "Feb")
            put(3L, "Mar")
            put(4L, "Apr")
            put(5L, "May")
            put(6L, "Jun")
            put(7L, "Jul")
            put(8L, "Aug")
            put(9L, "Sep")
            put(10L, "Oct")
            put(11L, "Nov")
            put(12L, "Dec")
        }
        private val obsoleteZones = HashMap<String, String>().apply {
            put("UT", "GMT")
            put("EDT", "-0400")
            put("EST", "-0500")
            put("CST", "-0600")
            put("CDT", "-0500")
            put("MST", "-0700")
            put("MDT", "-0600")
            put("PST", "-0800")
            put("PDT", "-0700")
        }

        // Based on DateTimeFormatter.RFC_1123_DATE_TIME.
        private val RFC_1123_WITH_NO_ZONE = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .optionalStart()
            .appendText(ChronoField.DAY_OF_WEEK, dow)
            .appendLiteral(", ")
            .optionalEnd()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(' ')
            .appendText(ChronoField.MONTH_OF_YEAR, moy)
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4)  // 2 digit year not handled
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .appendLiteral(' ')
            .toFormatter()

        @VisibleForTesting
        fun String?.parseRssDate(): Instant? =
            this?.let { dateString ->
                try {
                    val parsed = try {
                        ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
                    } catch(e : DateTimeParseException) {
                        // Parse obsolete time zones.
                        val zoneString = dateString.substringAfterLast(" ")
                        val localDateString = dateString.dropLast(zoneString.length)
                        val localDate = LocalDateTime.parse(localDateString, RFC_1123_WITH_NO_ZONE)
                        val zone = ZoneId.of(zoneString, obsoleteZones)
                        ZonedDateTime.of(localDate, zone)
                    }
                    parsed.toInstant()
                } catch (e: DateTimeException) {
                    Timber.w(e, "Failed to parse RSS date: '$dateString'")
                    Sentry.captureException(e)
                    null
                }
            }
    }
}