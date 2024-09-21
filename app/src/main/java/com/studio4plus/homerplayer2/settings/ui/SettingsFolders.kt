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

package com.studio4plus.homerplayer2.settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersManagementPanel
import com.studio4plus.homerplayer2.audiobookfoldersui.OpenAudiobooksTreeScreenWrapper
import com.studio4plus.homerplayer2.audiobookfoldersui.audiobooksFolderPanelErrorEventMessage
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsFoldersRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SettingsFoldersViewModel = koinViewModel()
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchErrorSnackDisplay(viewModel.errorEvent, snackbarHostState) {
        audiobooksFolderPanelErrorEventMessage(context, it)
    }

    OpenAudiobooksTreeScreenWrapper(
        onFolderSelected = { uri -> viewModel.addFolder(uri) },
    ) { openAudiobooksTree ->
        AudiobookFoldersManagementPanel(
            state = viewState,
            onAddFolder = {
                viewModel.clearErrorSnack()
                openAudiobooksTree()
            },
            onRemoveFolder = viewModel::removeFolder,
            onDownloadSamples = viewModel::startSamplesInstall,
            modifier = modifier.padding(horizontal = HomerTheme.dimensions.screenContentPadding),
        )
    }
}