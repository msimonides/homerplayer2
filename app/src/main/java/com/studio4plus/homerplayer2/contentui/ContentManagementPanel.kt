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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
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
    state: ContentPanelViewState?,
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
    if (state == null) {
        Box(modifier)
        return
    }

    Column(
        modifier = modifier
    ) {
        var addDialog by rememberSaveable { mutableStateOf(false) }
        var removeDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        Spacer(modifier = Modifier.windowInsetsTopHeight(windowInsets))

        if (state.folders.isEmpty() && state.podcasts.isEmpty()) {
            EmptyState(
                onAddContent = { addDialog = true },
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxSize()
            )
        } else {
            Button(
                onClick = { addDialog = true },
                modifier = Modifier.padding(horizontal = horizontalPadding)
            ) {
                Text(stringResource(R.string.content_add_button))
            }
            val insetsPadding = PaddingValues(
                bottom = windowInsets.asPaddingValues().calculateBottomPadding()
            )
            ContentColumn(
                state = state,
                onEditFolder = onEditFolder,
                onRemoveFolder = { item -> removeDialogAction = { onRemoveFolder(item) } },
                onEditPodcast = onEditPodcast,
                onRemovePodcast = { item -> removeDialogAction = { onRemovePodcast(item) } },
                contentPadding = insetsPadding,
                horizontalItemPadding = horizontalPadding,
                modifier = Modifier.consumeWindowInsets(insetsPadding)
            )
        }

        if (addDialog) {
            AddContentDialog(
                showSamples = state.samplesInstallState is SamplesInstallState.Idle,
                onDismiss = { addDialog = false },
                onAddFolder = onAddFolder,
                onAddPodcast = onAddPodcast,
                onDownloadSamples = onDownloadSamples,
                onLearnMoreFolders = {},
                onLearnMorePodcasts = {},
            )
        }
        if (removeDialogAction != null) {
            ConfirmRemoveDialog(
                onRemove = { removeDialogAction?.invoke() },
                onDismiss = { removeDialogAction = null })
        }
    }
}

@Composable
private fun ContentColumn(
    state: ContentPanelViewState,
    onEditFolder: (folderUri: String) -> Unit,
    onRemoveFolder: (AudiobookFolderViewState) -> Unit,
    onEditPodcast: (feedUri: String) -> Unit,
    onRemovePodcast: (PodcastItemViewState) -> Unit,
    contentPadding: PaddingValues,
    horizontalItemPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        val itemPaddingModifier = Modifier.padding(horizontal = horizontalItemPadding, vertical = 4.dp)
        if (state.folders.isNotEmpty()) {
            item {
                SectionTitle(
                    R.string.content_section_title_audiobooks,
                    modifier = itemPaddingModifier
                )
            }
            items(
                state.folders,
                key = AudiobookFolderViewState::uri,
                itemContent = { item ->
                    AudiobookFolderRow(
                        folder = item,
                        onEditClicked = onEditFolder,
                        onRemoveClicked = { onRemoveFolder(item) },
                        modifier = itemPaddingModifier
                    )
                }
            )
        }
        if (state.podcasts.isNotEmpty()) {
            item {
                SectionTitle(
                    R.string.content_section_title_podcasts,
                    modifier = itemPaddingModifier
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
                        onRemoveClicked = { onRemovePodcast(item) },
                        modifier = itemPaddingModifier
                    )
                }
            )
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

@Composable
private fun EmptyState(
    onAddContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = BiasAlignment(0f, -0.4f),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 480.dp)
            ) {
                Icon(
                    painterResource(R.drawable.icon_music_note_add),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .size(56.dp)
                )
                val text = buildAnnotatedString {
                    append(stringResource(R.string.content_empty_list_message))
                    append('\n')
                    listOf(
                        R.string.content_empty_list_message_bullet_1,
                        R.string.content_empty_list_message_bullet_2,
                        R.string.content_empty_list_message_bullet_3,
                    ).forEach {
                        append(" \u2022 ")
                        append(stringResource(it))
                        append('\n')
                    }
                }
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onAddContent) {
                    Text(stringResource(R.string.content_empty_list_button_add))
                }
            }
        }
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

@Preview(widthDp = 360, heightDp = 480)
@Composable
private fun PreviewContentManagementPanelEmpty() {
    HomerPlayer2Theme {
        val viewState = ContentPanelViewState(emptyList(), emptyList(), SamplesInstallState.Idle)
        ContentManagementPanel(viewState, {}, {}, {}, {}, {}, {}, {})
    }
}
