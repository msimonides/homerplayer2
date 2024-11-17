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

package com.studio4plus.homerplayer2.audiobookfoldersui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import io.sentry.Sentry
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
private fun OpenDocumentTreeFailedDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Text(stringResource(id = R.string.audiobook_folder_add_error_dialog))
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.padding(top = 16.dp).align(Alignment.End)
            ) {
                Text(stringResource(id = BaseR.string.generic_dialog_confirm))
            }
        }
    }
}

typealias OpenAudiobooksTreeAction = () -> Unit

@Composable
fun OpenAudiobooksTreeScreenWrapper(
    onFolderSelected: (Uri) -> Unit,
    content: @Composable (OpenAudiobooksTreeAction) -> Unit,
) {
    val openAudiobooksTree = rememberLauncherForActivityResult(
        contract = OpenAudiobooksTree(),
        onResult = { uri -> uri?.let { onFolderSelected(uri) } }
    )
    var openAudiobooksTreeError by rememberSaveable { mutableStateOf(false) }
    fun onOpenAudiobooksTreeError(e: Throwable) {
        openAudiobooksTreeError = true
        Sentry.captureException(e)
    }

    val openAudiobooksTreeAction: () -> Unit = {
        runCatching { openAudiobooksTree.launch(null) }
            .onFailure(::onOpenAudiobooksTreeError)
    }
    content(openAudiobooksTreeAction)

    if (openAudiobooksTreeError) {
        OpenDocumentTreeFailedDialog(onDismissRequest = { openAudiobooksTreeError = false })
    }
}