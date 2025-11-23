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

package com.studio4plus.homerplayer2.contentui

import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersViewStateFlow
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderViewState
import com.studio4plus.homerplayer2.podcastsui.PodcastItemViewState
import com.studio4plus.homerplayer2.podcastsui.PodcastsViewStateFlow
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallController
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

data class ContentPanelViewState(
    val folders: List<AudiobookFolderViewState>,
    val podcasts: List<PodcastItemViewState>,
    val samplesInstallState: SamplesInstallState?,
) {
    val isEmpty =
        folders.isEmpty() && podcasts.isEmpty() && samplesInstallState == SamplesInstallState.Idle
}

@Factory
class ContentPanelViewStateFlow(
    audiobookFoldersViewStateFlow: AudiobookFoldersViewStateFlow,
    podcastsViewStateFlow: PodcastsViewStateFlow,
    samplesInstallController: SamplesInstallController,
) : Flow<ContentPanelViewState> {

    private val flow = combine(
        audiobookFoldersViewStateFlow,
        podcastsViewStateFlow,
        samplesInstallController.stateFlow,
    ) { folders, podcasts, samplesState ->
        ContentPanelViewState(
            folders,
            podcasts,
            samplesState.takeIf { folders.all { it.samplesFolderState == null } }
        )
    }

    override suspend fun collect(collector: FlowCollector<ContentPanelViewState>) {
        flow.collect(collector)
    }


}