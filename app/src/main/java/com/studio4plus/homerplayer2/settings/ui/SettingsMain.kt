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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMainRoute(
    navigateFolders: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigatePlayerUiSettings: () -> Unit,
    navigateLockdownSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    navigateAbout: () -> Unit,
    viewModel: SettingsMainViewModel = koinViewModel()
) {
    SettingsMain(
        viewModel.viewState.collectAsStateWithLifecycle().value,
        onSetUiMode = viewModel::setUiMode,
        navigateFolders = navigateFolders,
        navigatePlaybackSettings = navigatePlaybackSettings,
        navigatePlayerUiSettings = navigatePlayerUiSettings,
        navigateLockdownSettings = navigateLockdownSettings,
        navigateTtsSettings = navigateTtsSettings,
        navigateAbout = navigateAbout,
    )
}

private enum class SettingsMainDialogType {
    UiMode
}

@Composable
private fun SettingsMain(
    viewState: SettingsMainViewModel.ViewState?,
    onSetUiMode: (UiThemeMode) -> Unit,
    navigateFolders: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigatePlayerUiSettings: () -> Unit,
    navigateLockdownSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    navigateAbout: () -> Unit,
) {
    if (viewState != null) {
        var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsMainDialogType?>(null) }
        Column {
            val settingItemModifier = Modifier.defaultSettingsItem()
            SettingItem(
                label = stringResource(R.string.settings_ui_player_ui_item),
                onClick = navigatePlayerUiSettings,
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
                label = stringResource(R.string.settings_ui_audiobooks_folders_item),
                summary = viewState.audiobookFolders ?: stringResource(R.string.settings_ui_audiobooks_folders_summary_empty),
                onClick = navigateFolders,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_tts_settings_item),
                summary = stringResource(ttsSummaryRes),
                onClick = navigateTtsSettings,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_mode_label),
                summary = stringResource(viewState.uiMode.labelRes()),
                onClick = { showUiModeDialog = SettingsMainDialogType.UiMode },
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_lockdown_settings_item),
                onClick = navigateLockdownSettings,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_about_item),
                onClick = navigateAbout,
                modifier = settingItemModifier
            )
        }

        val dismissAction = { showUiModeDialog = null }
        when (showUiModeDialog) {
            SettingsMainDialogType.UiMode -> ChooseUiModeDialog(
                value = viewState.uiMode,
                onValueChange = onSetUiMode,
                onDismissRequest = dismissAction
            )
            null -> Unit
        }
    }
}

@Composable
private fun ChooseUiModeDialog(
    value: UiThemeMode,
    onValueChange: (UiThemeMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    SelectFromRadioListDialog(
        selectedValue = value,
        values = listOf(UiThemeMode.SYSTEM, UiThemeMode.LIGHT, UiThemeMode.DARK),
        produceLabel = { stringResource(id = it.labelRes()) },
        title = stringResource(id = R.string.settings_ui_mode_label),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest
    )
}

private fun UiThemeMode.labelRes() = when(this) {
    UiThemeMode.SYSTEM -> R.string.settings_ui_mode_system
    UiThemeMode.LIGHT -> R.string.settings_ui_mode_light
    UiThemeMode.DARK -> R.string.settings_ui_mode_dark
}

@Preview
@Composable
private fun PreviewSettingsMain() {
    HomerPlayer2Theme {
        val viewState = SettingsMainViewModel.ViewState(
            audiobookFolders = "AudioBooks, Samples",
            ttsEnabled = true,
            uiMode = UiThemeMode.SYSTEM,
        )
        SettingsMain(viewState, {}, {}, {}, {}, {}, {}, {})
    }
}
