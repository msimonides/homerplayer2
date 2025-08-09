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

package com.studio4plus.homerplayer2.audiobooks

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class Audiobook(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "root_folder_uri")
    val rootFolderUri: Uri
)

@Entity(
    tableName = "audiobook_files",
    foreignKeys = [
        ForeignKey(
            entity = Audiobook::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class AudiobookFile(
    @PrimaryKey
    val uri: Uri,
    @ColumnInfo(name = "book_id", index = true)
    val bookId: String,
)

@Entity(
    tableName = "audiobook_file_durations",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookFile::class,
            parentColumns = ["uri"],
            childColumns = ["uri"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        )
    ]
)
data class AudiobookFileDuration(
    @PrimaryKey
    val uri: Uri,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long
) {
    companion object {
        const val INVALID: Long = -1
    }
}

@Entity(
    tableName = "audiobook_playback_states",
    foreignKeys = [
        ForeignKey(
            entity = Audiobook::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        )
    ]
)
// TODO: decide whether to keep state when a book is removed (and if so, for how long),
// TODO: consider adding a foreign key for currentUri.
data class AudiobookPlaybackState(
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "is_new", defaultValue = "0")
    val isNew: Boolean,
    @ColumnInfo(name = "current_uri")
    val currentUri: Uri,
    @ColumnInfo(name = "current_position_ms")
    val currentPositionMs: Long
)

@DatabaseView("""
    SELECT files.*, durations.duration_ms FROM audiobook_files AS files
        JOIN audiobook_file_durations AS durations ON files.uri = durations.uri
        ORDER BY files.uri""")
data class AudiobookFileWithDuration(
    @ColumnInfo(name = "book_id")
    val bookId: String,
    val uri: Uri,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?
)