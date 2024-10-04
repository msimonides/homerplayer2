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

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFoldersDatabase
import com.studio4plus.homerplayer2.audiobookfolders.AudiobooksFolder
import com.studio4plus.homerplayer2.audiobooks.Audiobook
import com.studio4plus.homerplayer2.audiobooks.AudiobookFile
import com.studio4plus.homerplayer2.audiobooks.AudiobookFileDuration
import com.studio4plus.homerplayer2.audiobooks.AudiobookFileWithDuration
import com.studio4plus.homerplayer2.audiobooks.AudiobookPlaybackState
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDatabase
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
    version = 3,
    autoMigrations = [
        AutoMigration(1, 2),
    ],
)
@TypeConverters(DbTypeConverters::class)
abstract class AppDatabase : RoomDatabase(), AudiobooksDatabase, AudiobookFoldersDatabase {

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val statements = listOf(
                    // Migrate audiobook_playback_states
                    "ALTER TABLE `audiobook_playback_states` RENAME TO `old_audiobook_playback_states`",
                    "CREATE TABLE IF NOT EXISTS `audiobook_playback_states` (`book_id` TEXT NOT NULL, `current_uri` TEXT NOT NULL, `current_position_ms` INTEGER NOT NULL, PRIMARY KEY(`book_id`), FOREIGN KEY(`book_id`) REFERENCES `audiobooks`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)",
                    """INSERT INTO `audiobook_playback_states`
                            SELECT `audiobooks`.`root_folder_uri` || '/' || `audiobooks`.`display_name` AS `book_id`, `current_uri`, `current_position_ms`
                              FROM `old_audiobook_playback_states`
                              JOIN `audiobooks` ON `old_audiobook_playback_states`.`book_id` = `audiobooks`.`id`
                        """,
                    "DROP TABLE `old_audiobook_playback_states`"
                )
                statements.forEach {
                    db.execSQL(it)
                }
            }
        }
    }
}