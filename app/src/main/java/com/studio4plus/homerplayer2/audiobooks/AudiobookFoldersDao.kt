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

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookFoldersDao {

    @Query("SELECT * FROM audiobooks_folders")
    fun getAll(): Flow<List<AudiobooksFolder>>

    @MapInfo(keyColumn = "folder_uri", valueColumn = "book_count")
    @Query("""
        SELECT audiobooks_folders.uri AS folder_uri, COUNT(audiobooks.id) AS book_count
        FROM  audiobooks_folders, audiobooks
        WHERE audiobooks_folders.uri = audiobooks.root_folder_uri
        GROUP BY audiobooks_folders.uri
        """)
    fun getAllWithBookCounts(): Flow<Map<String, Int>>

    @Insert
    suspend fun insert(folder: AudiobooksFolder)

    @Delete
    suspend fun delete(folder: AudiobooksFolder)
}