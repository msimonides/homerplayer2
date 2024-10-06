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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderRow
import com.studio4plus.homerplayer2.audiobookfoldersui.FolderItem
import com.studio4plus.homerplayer2.audiobookfoldersui.PreviewData
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState

@Composable
fun ContentManagementPanel(
    state: ContentPanelViewState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (FolderItem) -> Unit,
    onDownloadSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onAddFolder) {
                Text(stringResource(id = R.string.audiobook_folder_add_button))
            }
            if (state.samplesInstallState != null) {
                OutlinedButton(
                    onClick = onDownloadSamples,
                    enabled = state.samplesInstallState is SamplesInstallState.Idle
                ) {
                    Text(stringResource(id = R.string.audiobook_folder_download_samples_button))
                }
            }
        }
        LazyColumn {
            items(
                state.folders,
                key = FolderItem::uri,
                itemContent = { item ->
                    AudiobookFolderRow(item = item, onRemoveClicked = { onRemoveFolder(item) })
                }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewContentManagementPanel50() {
    HomerPlayer2Theme {
        ContentManagementPanel(
            ContentPanelViewState(PreviewData.folderItems50, SamplesInstallState.Idle),
            {}, {}, {}
        )
    }
}
