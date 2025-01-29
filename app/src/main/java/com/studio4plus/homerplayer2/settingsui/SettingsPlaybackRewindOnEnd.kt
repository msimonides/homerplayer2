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

package com.studio4plus.homerplayer2.settingsui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFoldersSettingsViewState
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.contentui.AudiobookFolderRow
import com.studio4plus.homerplayer2.contentui.BasicAudiobookFolderRow
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPlaybackRewindOnEndRoute(
    viewModel: SettingsPlaybackRewindOnEndViewModel = koinViewModel()
) {
    val settings = viewModel.settings.collectAsStateWithLifecycle(null).value
    val screenModifier = Modifier.fillMaxSize()
    if (settings != null) {
        SettingsPlaybackRewindOnEnd(settings, viewModel::setRewindOnEnd, screenModifier)
    } else {
        Box(screenModifier)
    }
}

@Composable
private fun SettingsPlaybackRewindOnEnd(
    settings: List<AudiobookFoldersSettingsViewState>,
    onToggle: (AudiobookFoldersSettingsViewState, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.settings_ui_playback_rewind_on_end_description),
            modifier = Modifier
                .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
                .padding(bottom = 8.dp)
        )
        SectionTitle(
            R.string.content_section_title_audiobooks,
            modifier = Modifier.padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
        )
        val settingItemModifier = Modifier.defaultSettingsItem()
        LazyColumn(
            modifier = Modifier.navigationBarsPadding()
        ) {
            items(settings, key = { it.audiobookFolderViewState.uri }) { item ->
                BasicAudiobookFolderRow(
                    folder = item.audiobookFolderViewState,
                    actionContent = @Composable {
                        Switch(
                            checked = item.settings.rewindOnEnd,
                            onCheckedChange = null,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    },
                    modifier = settingItemModifier
                        .toggleable(
                            value = item.settings.rewindOnEnd,
                            onValueChange = { onToggle(item, it) },
                        )
                )
            }
        }
    }
}