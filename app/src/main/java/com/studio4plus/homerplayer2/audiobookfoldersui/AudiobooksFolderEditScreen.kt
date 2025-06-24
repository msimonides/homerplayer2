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

package com.studio4plus.homerplayer2.audiobookfoldersui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.player.ui.HorizontalBookProgressIndicator
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import com.studio4plus.homerplayer2.settingsui.defaultSettingsItem
import org.koin.androidx.compose.koinViewModel
import kotlin.Long

@Composable
fun AudiobooksFolderEditRoute(
    viewModel: AudiobooksFolderEditViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    val screenModifier = modifier.fillMaxSize()
    if (viewState != null) {
        AudiobooksFolderEditScreen(
            viewState = viewState,
            onRewindOnEndChanged = viewModel::changeOnRewindOnEnd,
            modifier = screenModifier
        )
    } else {
        Box(screenModifier)
    }
}

@Composable
private fun AudiobooksFolderEditScreen(
    viewState: AudiobooksFolderEditViewModel.ViewState,
    onRewindOnEndChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val useWideRows = this.maxWidth >= 600.dp // TODO: or maybe use window size class instead?
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val rowModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
            Text(
                viewState.folderName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = rowModifier
            )

            SettingSwitch(
                stringResource(R.string.settings_ui_playback_rewind_on_end_title),
                summary = stringResource(R.string.settings_ui_playback_rewind_on_end_description),
                multilineSummary = true,
                value = viewState.rewindOnEnd,
                icon = Icons.Default.RestartAlt,
                onChange = onRewindOnEndChanged,
                modifier = Modifier.defaultSettingsItem()
            )

            LazyColumn {
                item {
                    SectionTitle(
                        stringResource(R.string.audiobook_folder_contents_title),
                        modifier = Modifier.padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
                    )
                }
                itemsIndexed(viewState.audiobooks, key = { _, item -> item.id }) { index, item ->
                    AudiobookRow(
                        title = item.displayName,
                        currentPositionMs = item.currentPositionMs,
                        totalDurationMs = item.totalDurationMs,
                        useWideLayout = useWideRows,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
                            .padding(top = 8.dp, bottom = 16.dp)
                    )
                }
                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

// TODO: extract a common list item for audiobook and podcast (and maybe more?)
@Composable
private fun AudiobookRow(
    title: String,
    currentPositionMs: Long?,
    totalDurationMs: Long?,
    useWideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        val positionAndDuration = if (currentPositionMs != null && totalDurationMs != null) {
            stringResource(
                R.string.audiobook_folder_content_time_format,
                formatPeriod(currentPositionMs),
                formatPeriod(totalDurationMs)
            )
        } else {
            stringResource(R.string.audiobook_folder_content_time_scanning)
        }
        if (useWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = positionAndDuration,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = positionAndDuration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        val progress = (currentPositionMs?.toFloat() ?: 0f) /
                (totalDurationMs?.takeIf { it > 0 }?.toFloat() ?: 1f)
        HorizontalBookProgressIndicator(
            progress = progress,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
            thickness = 3.dp,
        )
    }
}

@Composable
private fun formatPeriod(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val locale = LocalConfiguration.current.locale

    return String.format(locale, "%d:%02d:%02d", hours, minutes - hours * 60, seconds - minutes * 60)
}