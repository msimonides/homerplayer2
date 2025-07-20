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

package com.studio4plus.homerplayer2.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppDatabaseMigrationTests {

    private val DB_NAME = "test.db"

    @get:Rule
    val migrationHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun all_migrations_with_empty_DB() {
        migrationHelper.createDatabase(DB_NAME, 1).apply {
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            DB_NAME
        ).addMigrations(*AppDatabase.migrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    @Test
    fun episode_name_number_migrated_to_date() {
        val initSql = listOf(
            """INSERT INTO podcasts (feed_uri,   title, include_episode_number, include_podcast_title, include_episode_title, download_episode_count)
                             VALUES ('feed1', 'title1',                      1,                     1,                     0,                      2)
            """.trimMargin(),
            """INSERT INTO podcasts (feed_uri,   title, include_episode_number, include_podcast_title, include_episode_title, download_episode_count)
                             VALUES ('feed2', 'title2',                      0,                     0,                     1,                      1)
            """.trimMargin()
        )
        migrationHelper.createDatabase(DB_NAME, 4).apply {
            initSql.forEach { execSQL(it) }
        }

        migrationHelper.runMigrationsAndValidate(DB_NAME, 6, true)

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            DB_NAME
        ).build()
        val allPodcasts = db.podcastsDao().getPodcasts()
        val expectedPodcasts = listOf(
            Podcast("feed1", "title1", null, true, true, false, 2),
            Podcast("feed2", "title2", null, false, false, true, 1)
        )
        assertEquals(expectedPodcasts, allPodcasts)
    }
}