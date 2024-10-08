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

package com.studio4plus.homerplayer2.onboarding

import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersPanelViewState
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFolderManager
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersViewStateFlow
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderPanelViewModel
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class OnboardingAudiobookFoldersViewModel(
    mainScope: CoroutineScope,
    audiobookFoldersViewStateFlow: AudiobookFoldersViewStateFlow,
    audiobookFolderManager: AudiobookFolderManager,
    samplesInstaller: SamplesInstallController,
    private val onboardingDelegate: OnboardingDelegate,
) : AudiobookFolderPanelViewModel(mainScope, audiobookFolderManager, samplesInstaller) {
    data class ViewState(
        val panelState: AudiobookFoldersPanelViewState,
        val canProceed: Boolean,
    )

    val viewState = audiobookFoldersViewStateFlow
        .map { ViewState(it, it.folders.isNotEmpty()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ViewState(AudiobookFoldersPanelViewState(emptyList(), null), false)
        )

    fun onFinished() {
        onboardingDelegate.onOnboardingFinished()
    }
}