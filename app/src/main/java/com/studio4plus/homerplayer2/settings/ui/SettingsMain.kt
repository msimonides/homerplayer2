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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.settings.UiThemeMode
import com.studio4plus.homerplayer2.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel

private enum class DialogType {
    UiMode, PlaybackRewindOnResume
}

@Composable
fun SettingsMain(
    viewModel: MainViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        var showUiModeDialog by remember { mutableStateOf<DialogType?>(null) }
        Column {
            val settingItemModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HomerTheme.dimensions.settingsRowMinHeight)
                .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
            SettingSwitch(
                label = stringResource(R.string.settings_ui_full_kiosk_mode_label),
                value = viewState.fullKioskMode,
                onChange = { isEnabled -> viewModel.setFullKioskMode(isEnabled) },
                modifier = settingItemModifier
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_hide_settings_button_label),
                value = viewState.hideSettingsButton,
                onChange = { isEnabled -> viewModel.setHideSettingsButton(isEnabled) },
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_mode_label),
                summary = stringResource(viewState.uiMode.labelRes()),
                onClick = { showUiModeDialog = DialogType.UiMode },
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(id = R.string.settings_playback_rewind_on_resume_label),
                summary = rewindOnResumeSettingString(seconds = viewState.rewindOnResumeSeconds),
                onClick = { showUiModeDialog = DialogType.PlaybackRewindOnResume },
                modifier = settingItemModifier
            )
        }
        val dismissAction = { showUiModeDialog = null }
        when (showUiModeDialog) {
            DialogType.UiMode -> ChooseUiModeDialog(
                value = viewState.uiMode,
                onValueChange = { viewModel.setUiMode(it) },
                onDismissRequest = dismissAction
            )
            DialogType.PlaybackRewindOnResume -> ChooseRewindOnResumeDialog(
                value = viewState.rewindOnResumeSeconds,
                onValueChange = { viewModel.setRewindOnResumeSeconds(it) },
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
    SelectFromListDialog(
        selectedValue = value,
        values = listOf(UiThemeMode.SYSTEM, UiThemeMode.LIGHT, UiThemeMode.DARK),
        produceLabel = { stringResource(id = it.labelRes()) },
        title = stringResource(id = R.string.settings_ui_mode_label),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest
    )
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

private fun UiThemeMode.labelRes() = when(this) {
    UiThemeMode.SYSTEM -> R.string.settings_ui_system
    UiThemeMode.LIGHT -> R.string.settings_ui_light
    UiThemeMode.DARK -> R.string.settings_ui_dark
}
