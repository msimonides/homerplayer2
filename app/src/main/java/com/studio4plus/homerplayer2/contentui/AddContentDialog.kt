/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.R as BaseR
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme

@Composable
fun AddContentDialog(
    showSamples: Boolean,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onAddPodcast: () -> Unit,
    onDownloadSamples: () -> Unit,
    onLearnMoreFolders: () -> Unit,
    onLearnMorePodcasts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(460.dp),
        usePlatformDefaultWidth = false,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(BaseR.string.generic_dialog_cancel))
            }
        }
    ) { horizontalPadding ->
        AddContentSelection(
            showSamples = showSamples,
            onAddFolder = { onAddFolder(); onDismiss() },
            onAddPodcast = { onAddPodcast(); onDismiss() },
            onDownloadSamples = { onDownloadSamples(); onDismiss() },
            onLearnMoreFolders = onLearnMoreFolders,
            onLearnMorePodcasts = onLearnMorePodcasts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )
    }
}

@Composable
private fun AddContentSelection(
    showSamples: Boolean,
    onAddFolder: () -> Unit,
    onAddPodcast: () -> Unit,
    onDownloadSamples: () -> Unit,
    onLearnMoreFolders: () -> Unit,
    onLearnMorePodcasts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val cardModifier = Modifier.fillMaxWidth()
        AddContentTypeCard(
            stringResource(R.string.content_add_dialog_folder_card_title),
            stringResource(R.string.content_add_dialog_folder_card_description),
            onClick = onAddFolder,
            onLearnMoreClick = onLearnMoreFolders,
            modifier = cardModifier,
        )

        AddContentTypeCard(
            stringResource(R.string.content_add_dialog_podcast_card_title),
            stringResource(R.string.content_add_dialog_podcast_card_description),
            onClick = onAddPodcast,
            onLearnMoreClick = onLearnMorePodcasts,
            modifier = cardModifier,
        )

        if (showSamples) {
            AddContentTypeCard(
                stringResource(R.string.content_add_dialog_samples_card_title),
                stringResource(R.string.content_add_dialog_samples_card_description),
                onClick = onDownloadSamples,
                modifier = cardModifier,
            )
        }
    }
}

@Composable
private fun AddContentTypeCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLearnMoreClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description)
            if (onLearnMoreClick != null) {
                Text(
                    "Learn more",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onLearnMoreClick)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewAddContentSelection() {
    HomerPlayer2Theme {
        AddContentDialog(
            showSamples = true,
            {}, {}, {}, {}, {}, {},
        )
    }
}