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

package com.studio4plus.homerplayer2.settingsui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellWifi
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import com.studio4plus.homerplayer2.settingsui.composables.SelectFromRadioListDialog
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.settingsui.usecases.ContentDescriptionFlow
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMainRoute(
    navigateFolders: () -> Unit,
    navigateLockdownSettings: () -> Unit,
    navigateNetworkSettings: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigatePlayerUiSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    navigateAbout: () -> Unit,
    viewModel: SettingsMainViewModel = koinViewModel()
) {
    SettingsMain(
        viewModel.viewState.collectAsStateWithLifecycle().value,
        onSetUiMode = viewModel::setUiMode,
        navigateFolders = navigateFolders,
        navigateLockdownSettings = navigateLockdownSettings,
        navigateNetworkSettings = navigateNetworkSettings,
        navigatePlaybackSettings = navigatePlaybackSettings,
        navigatePlayerUiSettings = navigatePlayerUiSettings,
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
    navigateLockdownSettings: () -> Unit,
    navigateNetworkSettings: () -> Unit,
    navigatePlaybackSettings: () -> Unit,
    navigatePlayerUiSettings: () -> Unit,
    navigateTtsSettings: () -> Unit,
    navigateAbout: () -> Unit,
) {
    if (viewState != null) {
        var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsMainDialogType?>(null) }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            val settingItemModifier = Modifier.Companion.defaultSettingsItem()
            SettingItem(
                label = stringResource(R.string.settings_ui_player_ui_item),
                onClick = navigatePlayerUiSettings,
                icon = Icons.Default.Tune,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_playback_settings_item),
                onClick = navigatePlaybackSettings,
                icon = Icons.Default.PlayArrow,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_content_item),
                summary = viewState.content.summary(),
                onClick = navigateFolders,
                icon = Icons.Default.LibraryMusic,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_network_item),
                onClick = navigateNetworkSettings,
                icon = Icons.Default.CellWifi,
                modifier = settingItemModifier
            )
            val ttsSummaryRes = when {
                viewState.ttsEnabled ->  R.string.settings_ui_tts_settings_enabled
                else -> R.string.settings_ui_tts_settings_disabled
            }
            SettingItem(
                label = stringResource(R.string.settings_ui_tts_settings_item),
                summary = stringResource(ttsSummaryRes),
                onClick = navigateTtsSettings,
                icon = Icons.Default.RecordVoiceOver,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_mode_label),
                summary = stringResource(viewState.uiMode.labelRes()),
                onClick = { showUiModeDialog = SettingsMainDialogType.UiMode },
                icon = Icons.Default.Contrast,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_lockdown_settings_item),
                onClick = navigateLockdownSettings,
                icon = Icons.Default.LockOpen,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_about_item),
                onClick = navigateAbout,
                icon = Icons.Default.Info,
                modifier = settingItemModifier
            )
            if (viewState.rateAppIntent != null) {
                val context = LocalContext.current
                SettingItem(
                    label = stringResource(R.string.settings_ui_rate_app_item),
                    onClick = { context.startActivity(viewState.rateAppIntent) },
                    icon = Icons.Default.RateReview,
                    modifier = settingItemModifier
                )
            }
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

@Composable
private fun ContentDescriptionFlow.Content.summary(): String {
    val audiobooksString = pluralStringResource(
        R.plurals.settings_ui_content_summary_audiobook_folders,
        audiobookFolders,
        audiobookFolders
    )
    val podcastsString = pluralStringResource(R.plurals.settings_ui_content_summary_pocasts, podcasts, podcasts)
    return when {
        audiobookFolders == 0 && podcasts == 0 -> stringResource(R.string.settings_ui_content_summary_empty)
        audiobookFolders == 0 -> podcastsString
        podcasts == 0 -> audiobooksString
        else -> stringResource(R.string.settings_ui_content_summary, audiobooksString, podcastsString)
    }
}

@Preview
@Composable
private fun PreviewSettingsMain() {
    HomerPlayer2Theme {
        val viewState = SettingsMainViewModel.ViewState(
            content = ContentDescriptionFlow.Content(audiobookFolders = 2, podcasts = 1),
            rateAppIntent = null,
            ttsEnabled = true,
            uiMode = UiThemeMode.SYSTEM,
        )
        SettingsMain(viewState, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
