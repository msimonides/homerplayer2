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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AudiobooksDao {

    @Query("SELECT * FROM audiobooks")
    abstract fun getAll(): Flow<List<Audiobook>>

    @Query("SELECT * FROM audiobook_files WHERE book_id = :id")
    abstract suspend fun getFilesForBook(id: String): List<AudiobookFile>?

    @Transaction
    open suspend fun replaceAll(newAudiobooks: List<Audiobook>, newFiles: List<AudiobookFile>) {
        // TODO: something more efficient?
        deleteAllAudiobookFiles()
        deleteAllAudiobooks()
        insertAudiobooks(newAudiobooks)
        insertAudiobookFiles(newFiles)
    }

    @Insert
    protected abstract suspend fun insertAudiobooks(audiobooks: List<Audiobook>)

    @Insert
    protected abstract suspend fun insertAudiobookFiles(audiobookFiles: List<AudiobookFile>)

    @Query("DELETE FROM audiobooks")
    protected abstract suspend fun deleteAllAudiobooks()

    @Query("DELETE FROM audiobook_files")
    protected abstract suspend fun deleteAllAudiobookFiles()
}