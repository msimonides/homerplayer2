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
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AudiobooksDao {

    data class AudiobookWithState(
        @Embedded val audiobook: Audiobook,
        @Relation(parentColumn = "id", entityColumn = "book_id")
        val playbackState: AudiobookPlaybackState?,
        @Relation(parentColumn = "id", entityColumn = "book_id")
        val files: List<AudiobookFileWithDuration>
    )

    @Transaction
    @Query("SELECT * FROM audiobooks ORDER BY display_name COLLATE LOCALIZED")
    abstract fun getAll(): Flow<List<AudiobookWithState>>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    abstract suspend fun getAudiobook(id: String): AudiobookWithState?

    @Query("""SELECT audiobook_files.*
              FROM audiobook_files
                LEFT JOIN audiobook_file_durations ON audiobook_files.uri = audiobook_file_durations.uri
              WHERE audiobook_file_durations.uri IS NULL
              LIMIT :maxCount""")
    abstract fun getFilesWithoutDuration(maxCount: Int): Flow<List<AudiobookFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAudiobookFileDurations(durations: List<AudiobookFileDuration>)

    @Transaction
    open suspend fun replaceAll(newAudiobooks: List<Audiobook>, newFiles: List<AudiobookFile>) {
        // TODO: something more efficient?
        deleteAllAudiobookFiles()
        deleteAllAudiobooks()
        insertAudiobooks(newAudiobooks)
        insertAudiobookFiles(newFiles)
        deleteOrphanedDurations()
        deleteOrphanedPlaybackStates()
    }

    @Transaction
    open suspend fun updatePlayPosition(uri: Uri, positionMs: Long) {
        val file = getAudiobookFile(uri)
        if (file != null) {
            updatePlaybackState(AudiobookPlaybackState(file.bookId, uri, positionMs))
        }
    }

    @Query("SELECT * FROM audiobook_files WHERE uri = :uri")
    abstract suspend fun getAudiobookFile(uri: Uri): AudiobookFile?

    @Upsert
    protected abstract suspend fun updatePlaybackState(state: AudiobookPlaybackState)

    @Insert
    protected abstract suspend fun insertAudiobooks(audiobooks: List<Audiobook>)

    @Insert
    protected abstract suspend fun insertAudiobookFiles(audiobookFiles: List<AudiobookFile>)

    @Query("DELETE FROM audiobooks")
    protected abstract suspend fun deleteAllAudiobooks()

    @Query("DELETE FROM audiobook_files")
    protected abstract suspend fun deleteAllAudiobookFiles()

    @Query("""DELETE FROM audiobook_file_durations
                  WHERE uri NOT IN (SELECT uri FROM audiobook_files)""")
    protected abstract suspend fun deleteOrphanedDurations()

    @Query("""DELETE FROM audiobook_playback_states
                  WHERE book_id NOT IN (SELECT id FROM audiobooks)""")
    protected abstract suspend fun deleteOrphanedPlaybackStates()
}