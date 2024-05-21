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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.PreviewData
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobooks.ui.AudiobookFoldersManagementPanel
import com.studio4plus.homerplayer2.audiobooks.ui.FolderItem
import com.studio4plus.homerplayer2.audiobooks.ui.OpenAudiobooksTreeScreenWrapper
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingAudiobookFoldersRoute(
    modifier: Modifier = Modifier,
    navigateNext: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: OnboardingAudiobookFoldersViewModel = koinViewModel()
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    OpenAudiobooksTreeScreenWrapper(
        onFolderSelected =  { uri -> viewModel.addFolder(uri)}
    ) { openAudiobooksTree ->
        OnboardingAudiobookFoldersScreen(
            viewState = viewState,
            modifier = modifier,
            navigateNext = {
                viewModel.onFinished()
                navigateNext()
            },
            navigateBack = navigateBack,
            addFolder = openAudiobooksTree,
            removeFolder = viewModel::removeFolder
        )
    }
}

@Composable
fun OnboardingAudiobookFoldersScreen(
    viewState: OnboardingAudiobookFoldersViewModel.ViewState,
    navigateNext: () -> Unit,
    navigateBack: () -> Unit,
    addFolder: () -> Unit,
    removeFolder: (FolderItem) -> Unit,
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
        }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .padding(HomerTheme.dimensions.screenContentPadding),
            viewState.folders,
            addFolder,
            removeFolder
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    folders: List<FolderItem>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (FolderItem) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.onboarding_audiobook_folders_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = stringResource(id = R.string.onboarding_audiobook_folders_description))

        AudiobookFoldersManagementPanel(
            folders = folders,
            onAddFolder = onAddFolder,
            onRemoveFolder = onRemoveFolder
        )
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen1() {
    HomerPlayer2Theme {
        val state = OnboardingAudiobookFoldersViewModel.ViewState(
            PreviewData.folderItems1,
            canProceed = true
        )
        OnboardingAudiobookFoldersScreen(state, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun PreviewOnboardingAudiobookFoldersScreen50() {
    HomerPlayer2Theme {
        val state = OnboardingAudiobookFoldersViewModel.ViewState(
            PreviewData.folderItems50,
            canProceed = true
        )
        OnboardingAudiobookFoldersScreen(state, {}, {}, {}, {})
    }
}
