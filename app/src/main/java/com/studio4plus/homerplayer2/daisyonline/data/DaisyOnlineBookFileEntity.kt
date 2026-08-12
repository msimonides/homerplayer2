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

package com.studio4plus.homerplayer2.daisyonline.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daisy_online_book_files",
    foreignKeys = [
        ForeignKey(
            entity = DaisyOnlineBookEntity::class,
            parentColumns = ["service_id", "content_id"],
            childColumns = ["service_id", "content_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("service_id", "content_id")],
)
data class DaisyOnlineBookFileEntity(
    @PrimaryKey
    @ColumnInfo(name = "remote_uri")
    val remoteUri: String,
    @ColumnInfo(name = "service_id")
    val serviceId: String,
    @ColumnInfo(name = "content_id")
    val contentId: String,
    @ColumnInfo(name = "file_id")
    val fileId: String,
    @ColumnInfo(name = "is_downloaded")
    val isDownloaded: Boolean = false,
)
