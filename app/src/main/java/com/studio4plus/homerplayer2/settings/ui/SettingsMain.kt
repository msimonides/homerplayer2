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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private enum class SettingsMainDialogType {
    PlaybackRewindOnResume
}

@Composable
fun SettingsMain(
    navigateFolders: () -> Unit,
    navigateUiSettings: () -> Unit,
    viewModel: SettingsMainViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsMainDialogType?>(null) }
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
                label = stringResource(R.string.settings_ui_audiobooks_folders),
                summary = viewState.audiobookFolders ?: stringResource(R.string.settings_ui_audiobooks_folders_summary_empty),
                onClick = navigateFolders,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(id = R.string.settings_playback_rewind_on_resume_label),
                summary = rewindOnResumeSettingString(seconds = viewState.rewindOnResumeSeconds),
                onClick = { showUiModeDialog = SettingsMainDialogType.PlaybackRewindOnResume },
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(id = R.string.settings_ui_share_diagnostic_log_title),
                summary = stringResource(id = R.string.settings_ui_share_diagnostic_log_summary),
                onClick = {
                    coroutineScope.launch {
                        val shareIntent = viewModel.shareDiagnosticLogs()
                        context.startActivity(shareIntent)
                    }
                },
                modifier = settingItemModifier

            )
        }
        val dismissAction = { showUiModeDialog = null }
        when (showUiModeDialog) {
            SettingsMainDialogType.PlaybackRewindOnResume -> ChooseRewindOnResumeDialog(
                value = viewState.rewindOnResumeSeconds,
                onValueChange = { viewModel.setRewindOnResumeSeconds(it) },
                onDismissRequest = dismissAction
            )
            null -> Unit
        }
    }
}

@Composable
private fun ChooseRewindOnResumeDialog(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    SelectFromListDialog(
        selectedValue = value,
        values = listOf(0, 5, 15, 30, 60),
        produceLabel = { rewindOnResumeSettingString(it) },
        title = stringResource(id = R.string.settings_playback_rewind_on_resume_label),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun rewindOnResumeSettingString(seconds: Int): String =
    when (seconds) {
        0 -> stringResource(id = R.string.settings_playback_rewind_on_resume_setting_disabled)
        else -> pluralStringResource(id = R.plurals.settings_playback_rewind_on_resume_setting, seconds, seconds)
    }
