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

package com.studio4plus.homerplayer2.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.studio4plus.homerplayer2.app.data.StoredAppState
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksModule
import com.studio4plus.homerplayer2.onboarding.OnboardingModule
import com.studio4plus.homerplayer2.player.PlayerModule
import com.studio4plus.homerplayer2.settings.SettingsModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

const val DATASTORE_APP_STATE = "appState"

@Module(
    includes = [
        AudiobooksModule::class,
        OnboardingModule::class,
        PlayerModule::class,
        SettingsModule::class
    ]
)
@ComponentScan("com.studio4plus.homerplayer2.app")
class AppModule {

    @Single
    @Named(DATASTORE_APP_STATE)
    fun appStateDatastore(appContext: Context): DataStore<StoredAppState> =
        DataStoreFactory.create(StoredAppStateSerializer()) {
            appContext.dataStoreFile("$DATASTORE_APP_STATE.pb")
        }


    @Single(binds = [AppDatabase::class, AudiobooksDatabase::class])
    fun database(appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database")
            .build()
}