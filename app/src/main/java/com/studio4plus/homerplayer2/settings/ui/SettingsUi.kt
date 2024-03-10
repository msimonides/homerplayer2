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

package com.studio4plus.homerplayer2.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import org.koin.androidx.compose.koinViewModel

private enum class SettingsUiDialogType {
    UiMode, HideSettingsConfirmation
}

@Composable
fun SettingsUiRoute(
    viewModel: SettingsUiViewModel = koinViewModel(),
    navigateKioskModeSettings: () -> Unit,
    closeSettings: () -> Unit,
) {
    SettingsUi(
        viewModel.viewState.collectAsStateWithLifecycle().value,
        onSetFullKioskMode = viewModel::setFullKioskMode,
        onSetHapticFeedback = viewModel::setHapticFeedback,
        onSetHideSettingsButton = viewModel::setHideSettingsButton,
        onSetShowSettingsButton = viewModel::setShowBatteryIndicator,
        onSetUiMode = viewModel::setUiMode,
        onOpenKioskModeDetails = navigateKioskModeSettings,
        closeSettings = closeSettings,
    )
}

@Composable
fun SettingsUi(
    viewState: SettingsUiViewModel.ViewState?,
    onOpenKioskModeDetails: () -> Unit,
    onSetHapticFeedback: (isEnabled: Boolean) -> Unit,
    onSetFullKioskMode: (isEnabled: Boolean) -> Unit,
    onSetHideSettingsButton: (hide: Boolean) -> Unit,
    onSetShowSettingsButton: (show: Boolean) -> Unit,
    onSetUiMode: (UiThemeMode) -> Unit,
    closeSettings: () -> Unit,
) {
    if (viewState != null) {
        var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsUiDialogType?>(null) }
        Column {
            val settingItemModifier = Modifier.defaultSettingsItem()
            if (viewState.fullKioskModeAvailable) {
                SettingSwitch(
                    label = stringResource(R.string.settings_ui_full_kiosk_mode_label),
                    value = viewState.fullKioskMode,
                    onChange = onSetFullKioskMode,
                    modifier = settingItemModifier
                )
            } else {
                SettingItem(
                    label = stringResource(R.string.settings_ui_full_kiosk_mode_more_label),
                    onClick = onOpenKioskModeDetails,
                    modifier = settingItemModifier
                )
            }
            SettingSwitch(
                label = stringResource(R.string.settings_ui_hide_settings_button_label),
                value = viewState.hideSettingsButton,
                onChange = { isEnabled ->
                    if (isEnabled) {
                        showUiModeDialog = SettingsUiDialogType.HideSettingsConfirmation
                    } else {
                        onSetHideSettingsButton(false)
                    }
                },
                modifier = settingItemModifier
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_show_battery),
                value = viewState.showBattery,
                onChange = onSetShowSettingsButton,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_mode_label),
                summary = stringResource(viewState.uiMode.labelRes()),
                onClick = { showUiModeDialog = SettingsUiDialogType.UiMode },
                modifier = settingItemModifier
            )
            if (viewState.enableHapticFeedback != null) {
                SettingSwitch(
                    label = stringResource(R.string.settings_ui_haptic_feedback_label),
                    value = viewState.enableHapticFeedback,
                    onChange = onSetHapticFeedback,
                    modifier = settingItemModifier,
                )
            }
        }

        val dismissAction = { showUiModeDialog = null }
        when (showUiModeDialog) {
            SettingsUiDialogType.UiMode -> ChooseUiModeDialog(
                value = viewState.uiMode,
                onValueChange = onSetUiMode,
                onDismissRequest = dismissAction
            )
            SettingsUiDialogType.HideSettingsConfirmation -> HideSettingsButtonConfirmationDialog(
                onConfirm = {
                    onSetHideSettingsButton(true)
                    dismissAction()
                    closeSettings()
                },
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

private fun UiThemeMode.labelRes() = when(this) {
    UiThemeMode.SYSTEM -> R.string.settings_ui_mode_system
    UiThemeMode.LIGHT -> R.string.settings_ui_mode_light
    UiThemeMode.DARK -> R.string.settings_ui_mode_dark
}
