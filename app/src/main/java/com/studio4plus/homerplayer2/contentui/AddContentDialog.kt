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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.R as BaseR

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
        AddContentCardsColumn(
            showSamples = if (showSamples) SamplesCard.ShowLast else SamplesCard.Hide,
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
