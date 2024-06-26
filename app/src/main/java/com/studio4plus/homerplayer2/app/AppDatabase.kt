/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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

package com.studio4plus.homerplayer2.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobookFileDuration
import com.studio4plus.homerplayer2.audiobooks.AudiobookFileWithDuration
import com.studio4plus.homerplayer2.audiobooks.AudiobookPlaybackState
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksFolder
import com.studio4plus.homerplayer2.utils.DbTypeConverters

@Database(
    entities = [
        Audiobook::class,
        AudiobookFile::class,
        AudiobookFileDuration::class,
        AudiobookPlaybackState::class,
        AudiobooksFolder::class
    ],
    views = [ AudiobookFileWithDuration::class ],
    version = 1
)
@TypeConverters(DbTypeConverters::class)
abstract class AppDatabase : RoomDatabase(), AudiobooksDatabase