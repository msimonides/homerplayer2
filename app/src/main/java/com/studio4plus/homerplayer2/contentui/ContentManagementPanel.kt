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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderViewState
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.podcastsui.PodcastItemViewState
import com.studio4plus.homerplayer2.samplebooks.SamplesInstallState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.studio4plus.homerplayer2.base.R as BaseR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentManagementPanel(
    state: ContentPanelViewState,
    onAddFolder: () -> Unit,
    onEditFolder: (folderUri: String) -> Unit,
    onRemoveFolder: (AudiobookFolderViewState) -> Unit,
    onAddPodcast: () -> Unit,
    onEditPodcast: (feedUri: String) -> Unit,
    onRemovePodcast: (PodcastItemViewState) -> Unit,
    onDownloadSamples: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
) {
    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(windowInsets))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        ) {
            Button(onClick = onAddFolder) {
                Text(stringResource(id = R.string.content_add_folder_button))
            }
            Button(onClick = onAddPodcast) {
                Text(stringResource(id = R.string.content_add_podcast_button))
            }
            if (state.samplesInstallState != null) {
                OutlinedButton(
                    onClick = onDownloadSamples,
                    enabled = state.samplesInstallState is SamplesInstallState.Idle
                ) {
                    Text(stringResource(id = R.string.content_download_samples_button))
                }
            }
        }
        var removeDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        val insetsPadding = PaddingValues(
            bottom = windowInsets.asPaddingValues().calculateBottomPadding()
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = insetsPadding,
            modifier = Modifier.consumeWindowInsets(insetsPadding)
        ) {
            val horizontalPaddingModifier = Modifier.padding(horizontal = horizontalPadding)
            if (state.folders.isNotEmpty()) {
                item {
                    SectionTitle(
                        R.string.content_section_title_audiobooks,
                        modifier = horizontalPaddingModifier
                    )
                }
                items(
                    state.folders,
                    key = AudiobookFolderViewState::uri,
                    itemContent = { item ->
                        AudiobookFolderRow(
                            folder = item,
                            onEditClicked = onEditFolder,
                            onRemoveClicked = { removeDialogAction = { onRemoveFolder(item) } },
                            modifier = horizontalPaddingModifier
                        )
                    }
                )
            }
            if (state.podcasts.isNotEmpty()) {
                item {
                    SectionTitle(
                        R.string.content_section_title_podcasts,
                        modifier = horizontalPaddingModifier
                    )
                }
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                items(
                    state.podcasts,
                    key = PodcastItemViewState::feedUri,
                    itemContent = { item ->
                        PodcastRow(
                            item,
                            dateFormatter,
                            onEditClicked = onEditPodcast,
                            onRemoveClicked = { removeDialogAction = { onRemovePodcast(item) } },
                            modifier = horizontalPaddingModifier
                        )
                    }
                )
            }
        }
        if (removeDialogAction != null) {
            ConfirmRemoveDialog(
                onRemove = { removeDialogAction?.invoke() },
                onDismiss = { removeDialogAction = null })
        }
    }
}

@Composable
private fun ConfirmRemoveDialog(
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultAlertDialog(
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onDismiss) { Text(stringResource(BaseR.string.generic_dialog_cancel)) }
            val removeAndDismiss = {
                onRemove()
                onDismiss()
            }
            TextButton(removeAndDismiss) {
                Text(stringResource(R.string.content_dialog_remove_button_remove))
            }
        }
    ) { horizontalPadding ->
        Text(
            stringResource(R.string.content_dialog_remove_message),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
    }
}

@Preview
@Composable
private fun PreviewContentManagementPanelPodcast() {
    HomerPlayer2Theme {
        val viewState =
            ContentPanelViewState(PreviewData.folderItems1, PreviewData.podcasts1, SamplesInstallState.Idle)
        ContentManagementPanel(viewState, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun PreviewContentManagementPanel50() {
    HomerPlayer2Theme {
        val viewState =
            ContentPanelViewState(PreviewData.folderItems50, emptyList(), SamplesInstallState.Idle)
        ContentManagementPanel(viewState, {}, {}, {}, {}, {}, {}, {})
    }
}
