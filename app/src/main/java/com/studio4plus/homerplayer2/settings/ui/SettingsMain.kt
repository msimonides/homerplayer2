/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMainRoute(
    navigateFolders: () -> Unit,
    navigateUiSettings: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    viewModel: SettingsMainViewModel = koinViewModel()
) {
    SettingsMain(
        viewModel.viewState.collectAsStateWithLifecycle().value,
        navigateFolders,
        navigateUiSettings,
        navigatePlaybackSettings,
        navigateTtsSettings,
        viewModel::shareDiagnosticLogsIntent,
    )
}

@Composable
private fun SettingsMain(
    viewState: SettingsMainViewModel.ViewState?,
    navigateFolders: () -> Unit,
    navigateUiSettings: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    shareDiagnosticLogIntent: suspend () -> Intent,
) {
    if (viewState != null) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        Column {
            val settingItemModifier = Modifier.defaultSettingsItem()
            SettingItem(
                label = stringResource(R.string.settings_ui_ui_settings_item),
                onClick = navigateUiSettings,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_playback_settings_item),
                onClick = navigatePlaybackSettings,
                modifier = settingItemModifier,
            )
            val ttsSummaryRes = when {
                viewState.ttsEnabled ->  R.string.settings_ui_tts_settings_enabled
                else -> R.string.settings_ui_tts_settings_disabled
            }
            SettingItem(
                label = stringResource(R.string.settings_ui_tts_settings_item),
                summary = stringResource(ttsSummaryRes),
                onClick = navigateTtsSettings,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_audiobooks_folders),
                summary = viewState.audiobookFolders ?: stringResource(R.string.settings_ui_audiobooks_folders_summary_empty),
                onClick = navigateFolders,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(id = R.string.settings_ui_share_diagnostic_log_title),
                summary = stringResource(id = R.string.settings_ui_share_diagnostic_log_summary),
                onClick = {
                    coroutineScope.launch {
                        val shareIntent = shareDiagnosticLogIntent()
                        context.startActivity(shareIntent)
                    }
                },
                modifier = settingItemModifier
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSettingsMain() {
    HomerPlayer2Theme {
        val viewState = SettingsMainViewModel.ViewState(
            audiobookFolders = "AudioBooks, Samples",
            ttsEnabled = true,
        )
        SettingsMain(viewState, {}, {}, {}, {}, { Intent() })
    }
}
