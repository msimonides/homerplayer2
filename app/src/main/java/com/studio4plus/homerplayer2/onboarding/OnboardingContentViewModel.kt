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
import com.studio4plus.homerplayer2.audiobookfolders.AudiobookFolderManager
import com.studio4plus.homerplayer2.contentui.ContentPanelViewModel
import com.studio4plus.homerplayer2.contentui.ContentPanelViewState
import com.studio4plus.homerplayer2.contentui.ContentPanelViewStateFlow
import com.studio4plus.homerplayer2.podcasts.usecases.DeletePodcast
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class OnboardingContentViewModel(
    mainScope: CoroutineScope,
    contentPanelViewStateFlow: ContentPanelViewStateFlow,
    audiobookFolderManager: AudiobookFolderManager,
    deletePodcast: DeletePodcast,
    samplesInstaller: SamplesInstallController,
) : ContentPanelViewModel(mainScope, audiobookFolderManager, deletePodcast, samplesInstaller) {
    data class ViewState(
        val panelState: ContentPanelViewState?,
        val canProceed: Boolean,
    )

    val viewState = contentPanelViewStateFlow
        .map { ViewState(it, it.folders.isNotEmpty() || it.podcasts.isNotEmpty()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ViewState(null, false)
        )
}