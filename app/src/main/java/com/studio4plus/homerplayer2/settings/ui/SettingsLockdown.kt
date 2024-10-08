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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskEnabled
import com.studio4plus.homerplayer2.settingsdata.FullKioskModeSetting
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter


private enum class SettingsLockdownDialogType {
    ChangeKioskMode, HideSettingsConfirmation
}

@Composable
fun SettingsLockdownRoute(
    viewModel: SettingsLockdownViewModel = koinViewModel(),
    navigateKioskModeSettings: () -> Unit,
    closeSettings: () -> Unit,
) {
    SettingsLockdown(
        viewState = viewModel.viewState.collectAsStateWithLifecycle(null).value,
        onSetFullKioskMode = viewModel::setFullKioskMode,
        onOpenKioskModeSetup = navigateKioskModeSettings,
        onSetHideSettingsButton = viewModel::setHideSettingsButton,
        onSetShowSettingsButton = viewModel::setShowBatteryIndicator,
        closeSettings = closeSettings
    )
}

@Composable
fun SettingsLockdown(
    viewState : SettingsLockdownViewModel.ViewState?,
    onSetFullKioskMode: (setting: SettingsLockdownViewModel.FullKioskModeSetValue) -> Unit,
    onOpenKioskModeSetup: () -> Unit,
    onSetHideSettingsButton: (hide: Boolean) -> Unit,
    onSetShowSettingsButton: (show: Boolean) -> Unit,
    closeSettings: () -> Unit,
) {
    var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsLockdownDialogType?>(null) }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        val settingItemModifier = Modifier.defaultSettingsItem()
        Text(
            stringResource(R.string.settings_ui_lockdown_settings_description),
            modifier = Modifier
                .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
                .padding(bottom = 8.dp)
        )
        if (viewState != null) {
            val kioskModeAction = if (viewState.fullKioskModeAvailable) {
                { showUiModeDialog = SettingsLockdownDialogType.ChangeKioskMode }
            } else {
                onOpenKioskModeSetup
            }
            SettingItem(
                label = stringResource(R.string.settings_ui_lockdown_kiosk_mode_item),
                summary = viewState.fullKioskMode.toSummary(),
                onClick = kioskModeAction,
                modifier = settingItemModifier
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_lockdown_hide_settings_button_item),
                value = viewState.hideSettingsButton,
                onChange = { isEnabled ->
                    if (isEnabled) {
                        showUiModeDialog = SettingsLockdownDialogType.HideSettingsConfirmation
                    } else {
                        onSetHideSettingsButton(false)
                    }
                },
                modifier = settingItemModifier
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_lockdown_show_battery),
                value = viewState.showBattery,
                onChange = onSetShowSettingsButton,
                modifier = settingItemModifier,
            )

            val dismissAction = { showUiModeDialog = null }
            when (showUiModeDialog) {
                SettingsLockdownDialogType.ChangeKioskMode -> KioskModeSelectionDialog(
                    onValueChange = onSetFullKioskMode,
                    onDismissRequest = dismissAction
                )
                SettingsLockdownDialogType.HideSettingsConfirmation -> HideSettingsButtonConfirmationDialog(
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
}

@Composable
fun IsFullKioskEnabled.Value.toSummary(): String {
    val locale = LocalConfiguration.current.locale
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    return when (this) {
        IsFullKioskEnabled.Enabled ->
            stringResource(R.string.settings_ui_lockdown_kiosk_mode_setting_enabled)

        IsFullKioskEnabled.Disabled ->
            stringResource(R.string.settings_ui_lockdown_kiosk_mode_setting_disabled)

        is IsFullKioskEnabled.DisabledUntil ->
            stringResource(
                R.string.settings_ui_lockdown_kiosk_mode_setting_disabled_until,
                timeFormatter.format(enableTime)
            )
    }
}

@Composable
private fun KioskModeSelectionDialog(
    onValueChange: (SettingsLockdownViewModel.FullKioskModeSetValue) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {

    SelectFromListDialog(
        title = stringResource(R.string.settings_ui_lockdown_kiosk_mode_dialog_title),
        values =  listOf(
            SettingsLockdownViewModel.FullKioskModeSetValue.Enable,
            SettingsLockdownViewModel.FullKioskModeSetValue.Disable,
            SettingsLockdownViewModel.FullKioskModeSetValue.DisableTemporarily
        ),
        produceLabel = @Composable { value ->
            val stringRes = when(value) {
                SettingsLockdownViewModel.FullKioskModeSetValue.Enable ->
                    R.string.settings_ui_lockdown_kiosk_mode_dialog_enable
                SettingsLockdownViewModel.FullKioskModeSetValue.Disable ->
                    R.string.settings_ui_lockdown_kiosk_mode_dialog_disable
                SettingsLockdownViewModel.FullKioskModeSetValue.DisableTemporarily ->
                    R.string.settings_ui_lockdown_kiosk_mode_dialog_disable_for_10_minutes
            }
            stringResource(stringRes)
        },
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    )
}
