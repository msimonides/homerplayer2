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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersManagementPanel
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersPanelViewState
import com.studio4plus.homerplayer2.audiobookfoldersui.FolderItem
import com.studio4plus.homerplayer2.audiobookfoldersui.OpenAudiobooksTreeScreenWrapper
import com.studio4plus.homerplayer2.audiobookfoldersui.PreviewData
import com.studio4plus.homerplayer2.audiobookfoldersui.audiobooksFolderPanelErrorEventMessage
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingAudiobookFoldersRoute(
    modifier: Modifier = Modifier,
    navigateNext: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: OnboardingAudiobookFoldersViewModel = koinViewModel()
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchErrorSnackDisplay(viewModel.errorEvent, snackbarHostState) {
        audiobooksFolderPanelErrorEventMessage(context, it)
    }

    OpenAudiobooksTreeScreenWrapper(
        onFolderSelected =  { uri -> viewModel.addFolder(uri)}
    ) { openAudiobooksTree ->
        OnboardingAudiobookFoldersScreen(
            viewState = viewState,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            navigateNext = {
                viewModel.onFinished()
                navigateNext()
            },
            navigateBack = navigateBack,
            addFolder = {
                viewModel.clearErrorSnack()
                openAudiobooksTree()
            },
            removeFolder = viewModel::removeFolder,
            downloadSamples = viewModel::startSamplesInstall,
        )
    }
}

@Composable
fun OnboardingAudiobookFoldersScreen(
    viewState: OnboardingAudiobookFoldersViewModel.ViewState,
    snackbarHostState: SnackbarHostState,
    navigateNext: () -> Unit,
    navigateBack: () -> Unit,
    addFolder: () -> Unit,
    removeFolder: (FolderItem) -> Unit,
    downloadSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            OnboardingNavigationButtons(
                nextEnabled = viewState.canProceed,
                nextLabel = R.string.onboarding_step_done,
                onNext = navigateNext,
                secondaryLabel = R.string.onboarding_step_back,
                onSecondary = navigateBack,
                modifier = Modifier.padding(OnboardingNavigationButtonsDefaults.paddingValues),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .padding(HomerTheme.dimensions.screenContentPadding),
            viewState.panelState,
            addFolder,
            removeFolder,
            downloadSamples,
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    panelState: AudiobookFoldersPanelViewState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (FolderItem) -> Unit,
    onDownloadSamples: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.onboarding_audiobook_folders_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = stringResource(id = R.string.onboarding_audiobook_folders_description))

        AudiobookFoldersManagementPanel(
            state = panelState,
            onAddFolder = onAddFolder,
            onRemoveFolder = onRemoveFolder,
            onDownloadSamples = onDownloadSamples,
        )
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen1() {
    HomerPlayer2Theme {
        val state = OnboardingAudiobookFoldersViewModel.ViewState(
            AudiobookFoldersPanelViewState(PreviewData.folderItems1, SamplesInstallState.Idle),
            canProceed = true
        )
        OnboardingAudiobookFoldersScreen(state, SnackbarHostState(), {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen50() {
    HomerPlayer2Theme {
        val state = OnboardingAudiobookFoldersViewModel.ViewState(
            AudiobookFoldersPanelViewState(PreviewData.folderItems50, SamplesInstallState.Idle),
            canProceed = true
        )
        OnboardingAudiobookFoldersScreen(state, SnackbarHostState(), {}, {}, {}, {}, {})
    }
}
