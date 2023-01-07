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

package com.studio4plus.homerplayer2.di

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.studio4plus.homerplayer2.app.AppDatabase
import com.studio4plus.homerplayer2.app.MainActivityViewModel
import com.studio4plus.homerplayer2.app.OnboardingFinishedHandler
import com.studio4plus.homerplayer2.app.StoredAppStateSerializer
import com.studio4plus.homerplayer2.audiobooks.AudiobookFolderManager
import com.studio4plus.homerplayer2.audiobooks.AudiobooksUpdater
import com.studio4plus.homerplayer2.audiobooks.MediaDurationExtractor
import com.studio4plus.homerplayer2.audiobooks.Scanner
import com.studio4plus.homerplayer2.concurrency.DefaultDispatcherProvider
import com.studio4plus.homerplayer2.concurrency.DispatcherProvider
import com.studio4plus.homerplayer2.onboarding.OnboardingAudiobookFoldersViewModel
import com.studio4plus.homerplayer2.onboarding.OnboardingFinishedObserver
import com.studio4plus.homerplayer2.onboarding.OnboardingSpeechViewModel
import com.studio4plus.homerplayer2.player.ui.PlayerViewModel
import com.studio4plus.homerplayer2.speech.Speaker
import com.studio4plus.homerplayer2.speech.SpeakerTts
import kotlinx.coroutines.MainScope
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.*
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.Locale

// TODO: split into modules
val appModule = module {
    factory { Locale.getDefault() }
    factory { androidContext().contentResolver }
    single { MainScope() }

    single(named("appState")) {
        DataStoreFactory.create(StoredAppStateSerializer()) {
            androidContext().dataStoreFile("appState.pb")
        }
    }
    factory {
        OnboardingFinishedHandler(get(), get(named("appState")))
    } withOptions { bind<OnboardingFinishedObserver>() }

    singleOf(::AudiobooksUpdater) { createdAtStart() }
    singleOf(::MediaDurationExtractor) { createdAtStart() }

    factoryOf(::SpeakerTts) { bind<Speaker>() }
    singleOf(::DefaultDispatcherProvider) { bind<DispatcherProvider>() }

    // TODO: remove non-audio renderers, enable audio offload.
    //  consider: ConstantBitrateSeekingEnabled
    factory { ExoPlayer.Builder(androidContext()).build() }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "app_database")
            .build()
    }
    factory { get<AppDatabase>().audiobooksDao() }
    factory { get<AppDatabase>().audiobookFoldersDao() }
    factoryOf(::AudiobookFolderManager)
    singleOf(::Scanner)

    viewModelOf(::OnboardingSpeechViewModel)
    viewModelOf(::OnboardingAudiobookFoldersViewModel)
    viewModelOf(::PlayerViewModel)
    viewModel {
        MainActivityViewModel(get(named("appState")))
    }
}