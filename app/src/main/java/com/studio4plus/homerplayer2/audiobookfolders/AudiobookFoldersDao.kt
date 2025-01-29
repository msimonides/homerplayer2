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

package com.studio4plus.homerplayer2.audiobookfolders

import android.net.Uri
import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class AudiobookFolderWithSettings(
    @Embedded
    val folder: AudiobooksFolder,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri"
    )
    private val settingsValue: AudiobooksFolderSettings?
) {
    @Transient
    val settings = settingsValue ?: AudiobooksFolderSettings(uri = folder.uri)
}

@Dao
abstract class AudiobookFoldersDao {

    @Query("SELECT * FROM audiobooks_folders")
    abstract fun getAll(): Flow<List<AudiobooksFolder>>

    // TODO: TBH this belongs in the AudiobooksDao because it accesses both audiobooks_folders and
    //  audiobooks data.
    @Query("""
        SELECT audiobooks_folders.uri, audiobooks.display_name
        FROM  audiobooks_folders JOIN audiobooks ON audiobooks_folders.uri = audiobooks.root_folder_uri
        ORDER BY display_name COLLATE LOCALIZED
    """)
    abstract fun getAllWithBookTitles(): Flow<Map<@MapColumn(columnName = "uri") Uri, List<@MapColumn(columnName="display_name") String>>>

    @Transaction
    @Query("SELECT * FROM audiobooks_folders")
    abstract fun getAllFolderWithSettings(): Flow<List<AudiobookFolderWithSettings>>

    @Transaction
    @Query("SELECT * FROM audiobooks_folders WHERE uri = :uri")
    abstract fun getFolderWithSettings(uri: Uri): Flow<AudiobookFolderWithSettings>

    @Transaction
    suspend open fun updateFolderSettings(
        uri: Uri,
        transform: (AudiobooksFolderSettings) -> AudiobooksFolderSettings
    ) {
        val settings = getFolderSettings(uri) ?: AudiobooksFolderSettings(uri)
        upsertFolderSettings(transform(settings))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(folder: AudiobooksFolder): Long

    // Note: always call AudiobooksDao.deleteBooksFromFolder first!
    @Query("DELETE FROM audiobooks_folders WHERE uri = :uri")
    abstract suspend fun delete(uri: Uri)

    @Query("SELECT uri FROM audiobooks_folders WHERE isSamplesFolder = 1")
    abstract suspend fun getSamplesFolderUri(): Uri?

    @Query("SELECT * from audiobooks_folder_settings WHERE uri = :uri")
    protected abstract fun getFolderSettings(uri: Uri): AudiobooksFolderSettings?

    @Upsert
    protected abstract fun upsertFolderSettings(settings: AudiobooksFolderSettings)
}