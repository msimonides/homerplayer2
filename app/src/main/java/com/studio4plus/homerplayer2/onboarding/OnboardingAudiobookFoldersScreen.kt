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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobooks.OpenAudiobooksTree
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLifecycleComposeApi::class)
@Composable
fun OnboardingAudiobookFoldersScreen(
    modifier: Modifier = Modifier,
    navigateNext: () -> Unit,
    navigateBack: () -> Unit
) {
    val viewModel: OnboardingAudiobookFoldersViewModel = koinViewModel()
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()
    val openAudiobooksTree = rememberLauncherForActivityResult(
        contract = OpenAudiobooksTree(),
        onResult = { uri -> uri?.let { viewModel.addFolder(uri) } }
    )

    val currentViewState = viewState.value
    Scaffold(
        modifier = modifier,
        bottomBar = {
            OnboardingNavigationButtons(
                nextEnabled = currentViewState.canProceed,
                nextLabel = R.string.onboarding_step_done,
                onNext = navigateNext,
                secondaryLabel = R.string.onboarding_step_back,
                onSecondary = navigateBack,
                modifier = Modifier.padding(DefaultSpacing.ScreenContentPadding)
            )
        }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .padding(DefaultSpacing.ScreenContentPadding),
            currentViewState.folders,
            { openAudiobooksTree.launch(null) }
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    folders: List<OnboardingAudiobookFoldersViewModel.FolderItem>,
    onAddFolder: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.onboarding_audiobook_folders_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = stringResource(id = R.string.onboarding_audiobook_folders_description))

        Button(onClick = onAddFolder) {
            Text(stringResource(id = R.string.audiobook_folder_add_button))
        }
        LazyColumn {
            items(folders, key = OnboardingAudiobookFoldersViewModel.FolderItem::uri, itemContent = { item ->
                Row {
                    Text(item.displayName)
                    Text(item.bookCount?.toString()?.let { " ($it)" } ?: "")
                }
            })
        }
    }
}