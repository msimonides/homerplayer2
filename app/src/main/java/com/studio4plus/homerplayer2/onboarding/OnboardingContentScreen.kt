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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderViewState
import com.studio4plus.homerplayer2.audiobookfoldersui.OpenAudiobooksTreeScreenWrapper
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.contentui.ContentManagementPanel
import com.studio4plus.homerplayer2.contentui.ContentPanelViewModel
import com.studio4plus.homerplayer2.contentui.ContentPanelViewState
import com.studio4plus.homerplayer2.contentui.PreviewData
import com.studio4plus.homerplayer2.podcastsui.PodcastItemViewState
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingContentRoute(
    modifier: Modifier = Modifier,
    navigateEditFolder: (folderUri: String) -> Unit,
    navigateAddPodcast: () -> Unit,
    navigateEditPodcast: (feedUri: String) -> Unit,
    navigateNext: () -> Unit,
    viewModel: OnboardingContentViewModel = koinViewModel()
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchErrorSnackDisplay(viewModel.errorEvent, snackbarHostState) {
        ContentPanelViewModel.errorEventMessage(context, it)
    }

    OpenAudiobooksTreeScreenWrapper(
        onFolderSelected =  { uri -> viewModel.addFolder(uri)}
    ) { openAudiobooksTree ->
        OnboardingContentScreen(
            viewState = viewState,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            navigateNext = navigateNext,
            addFolder = {
                viewModel.clearErrorSnack()
                openAudiobooksTree()
            },
            editFolder = navigateEditFolder,
            removeFolder = viewModel::removeFolder,
            addPodcast = navigateAddPodcast,
            editPodcast = navigateEditPodcast,
            removePodcast = viewModel::removePodcast,
            downloadSamples = viewModel::startSamplesInstall,
        )
    }
}

@Composable
fun OnboardingContentScreen(
    viewState: OnboardingContentViewModel.ViewState,
    snackbarHostState: SnackbarHostState,
    navigateNext: () -> Unit,
    addFolder: () -> Unit,
    editFolder: (folderUri: String) -> Unit,
    removeFolder: (AudiobookFolderViewState) -> Unit,
    addPodcast: () -> Unit,
    editPodcast: (feedUri: String) -> Unit,
    removePodcast: (PodcastItemViewState) -> Unit,
    downloadSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            OnboardingNavigationButtons(
                nextEnabled = viewState.canProceed,
                nextLabel = R.string.onboarding_step_next,
                onNext = navigateNext,
                modifier = Modifier
                    .padding(OnboardingNavigationButtonsDefaults.paddingValues)
                    .navigationBarsPadding(),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = HomerTheme.dimensions.screenHorizExtraPadding)
                .padding(top = HomerTheme.dimensions.screenVertPadding),
            panelState = viewState.panelState,
            onAddFolder = addFolder,
            onEditFolder = editFolder,
            onRemoveFolder = removeFolder,
            onAddPodcast = addPodcast,
            onEditPodcast = editPodcast,
            onRemovePodcast = removePodcast,
            onDownloadSamples = downloadSamples,
        )
    }
}

@Composable
private fun ScreenContent(
    panelState: ContentPanelViewState,
    onAddFolder: () -> Unit,
    onEditFolder: (folderUri: String) -> Unit,
    onRemoveFolder: (AudiobookFolderViewState) -> Unit,
    onAddPodcast: () -> Unit,
    onEditPodcast: (feedUri: String) -> Unit,
    onRemovePodcast: (PodcastItemViewState) -> Unit,
    onDownloadSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OnboardingHeader(
            titleRes = R.string.onboarding_content_title,
            descriptionRes = R.string.onboarding_content_description,
            modifier = Modifier
                .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
                .padding(bottom = 16.dp)
        )

        ContentManagementPanel(
            state = panelState,
            onAddFolder = onAddFolder,
            onEditFolder = onEditFolder,
            onRemoveFolder = onRemoveFolder,
            onAddPodcast = onAddPodcast,
            onEditPodcast = onEditPodcast,
            onRemovePodcast = onRemovePodcast,
            onDownloadSamples = onDownloadSamples,
            horizontalPadding = HomerTheme.dimensions.screenHorizPadding,
        )
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen1() {
    HomerPlayer2Theme {
        val state = OnboardingContentViewModel.ViewState(
            ContentPanelViewState(PreviewData.folderItems1, PreviewData.podcasts1, SamplesInstallState.Idle),
            canProceed = true
        )
        OnboardingContentScreen(state, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen50() {
    HomerPlayer2Theme {
        val state = OnboardingContentViewModel.ViewState(
            ContentPanelViewState(PreviewData.folderItems50, emptyList(), SamplesInstallState.Idle),
            canProceed = true
        )
        OnboardingContentScreen(state, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}
