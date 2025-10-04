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

package com.studio4plus.homerplayer2.podcasts.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "podcast_episodes",
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["feed_uri"],
            childColumns = ["feed_uri"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [ Index("feed_uri"), Index("file_id") ]
)
data class PodcastEpisode(
    @PrimaryKey
    val uri: String,
    val title: String,
    @ColumnInfo(name = "pub_time")
    val publicationTime: Instant?, // TODO: make it non-nullable after a transition period.
    @ColumnInfo(name = "feed_uri")
    val feedUri: String,
    @ColumnInfo(name = "is_downloaded")
    val isDownloaded: Boolean,
    @ColumnInfo(name = "file_id")
    val fileId: String, // A safe and unique name for the episode's file.
)