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
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.room.Room
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFoldersDatabase
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFoldersModule
import com.studio4plus.homerplayer2.audiobooks.AudiobooksDatabase
import com.studio4plus.homerplayer2.audiobooks.AudiobooksModule
import com.studio4plus.homerplayer2.base.BaseModule
import com.studio4plus.homerplayer2.base.DefaultVersionUpdate
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.base.VersionUpdate
import com.studio4plus.homerplayer2.battery.BatteryModule
import com.studio4plus.homerplayer2.fullkioskmode.FullKioskModeModule
import com.studio4plus.homerplayer2.loccalstorage.LOCAL_STORAGE_JSON
import com.studio4plus.homerplayer2.loccalstorage.LocalStorageModule
import com.studio4plus.homerplayer2.loccalstorage.createDataStore
import com.studio4plus.homerplayer2.logging.LoggingModule
import com.studio4plus.homerplayer2.net.NetModule
import com.studio4plus.homerplayer2.onboarding.OnboardingModule
import com.studio4plus.homerplayer2.player.PlayerModule
import com.studio4plus.homerplayer2.podcasts.PodcastsModule
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDatabase
import com.studio4plus.homerplayer2.samplebooks.SamplesDownloader
import com.studio4plus.homerplayer2.settingsdata.SettingsDataModule
import com.studio4plus.homerplayer2.settingsui.SettingsUiModule
import com.studio4plus.homerplayer2.utils.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

const val DATASTORE_APP_STATE = "appState"

@Module(
    includes = [
        AudiobookFoldersModule::class,
        AudiobooksModule::class,
        BaseModule::class,
        BatteryModule::class,
        FullKioskModeModule::class,
        LocalStorageModule::class,
        LoggingModule::class,
        NetModule::class,
        OnboardingModule::class,
        PlayerModule::class,
        PodcastsModule::class,
        SettingsDataModule::class,
        SettingsUiModule::class,
    ]
)
@ComponentScan("com.studio4plus.homerplayer2.app")
class AppModule {

    @Single
    @Named(DATASTORE_APP_STATE)
    fun appStateDatastore(
        appContext: Context,
        mainScope: CoroutineScope,
        dispatcherProvider: DispatcherProvider,
        @Named(LOCAL_STORAGE_JSON) json: Json,
        versionUpdate: VersionUpdate,
    ): DataStore<StoredAppState> =
        createDataStore(
            appContext,
            mainScope,
            dispatcherProvider,
            json,
            DATASTORE_APP_STATE,
            StoredAppState(),
            StoredAppState.serializer(),
            migrations = listOf(
                StoredAppStateMigration1_2(versionUpdate)
            )
        )

    @Single(
        binds = [
            AppDatabase::class,
            AudiobookFoldersDatabase::class,
            AudiobooksDatabase::class,
            PodcastsDatabase::class,
        ]
    )
    fun database(appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database")
            .addMigrations(*AppDatabase.migrations)
            .build()

    @Factory
    fun wifiManager(appContext: Context): WifiManager = appContext.getSystemService()!!

    @Factory
    fun partialWakeLock(appContext: Context): WakeLock {
        val powerManager: PowerManager = appContext.getSystemService()!!
        val appTag = appContext.getString(R.string.app_tag)
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"$appTag:sleep timer")
    }

    @Factory(binds = [Clock::class])
    fun defaultClock(): Clock = DefaultClock()

    @Single
    @Named(SamplesDownloader.URL)
    fun samplesUrl(appContext: Context) = appContext.getString(R.string.samples_download_url)

    // Always create to make sure it updates current version.
    @Single(createdAtStart = true)
    fun versionUpdate(appContext: Context): VersionUpdate =
        DefaultVersionUpdate(appContext, BuildConfig.VERSION_CODE)
}
