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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskEnabled
import com.studio4plus.homerplayer2.settingsdata.ScreenOrientation
import com.studio4plus.homerplayer2.settingsui.composables.SelectFromListDialog
import com.studio4plus.homerplayer2.settingsui.composables.SelectFromRadioListDialog
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter


private enum class SettingsLockdownDialogType {
    ChangeKioskMode, HideSettingsConfirmation, HomeComponentMode, ScreenOrientation
}

@Composable
fun SettingsLockdownRoute(
    viewModel: SettingsLockdownViewModel = koinViewModel(),
    navigateKioskModeSettings: () -> Unit,
    navigateLayoutSettings: () -> Unit,
    closeSettings: () -> Unit,
) {
    SettingsLockdown(
        viewState = viewModel.viewState.collectAsStateWithLifecycle(null).value,
        onSetFullKioskMode = viewModel::setFullKioskMode,
        onOpenKioskModeSetup = navigateKioskModeSettings,
        onOpenLayoutSettings = navigateLayoutSettings,
        onSetHideSettingsButton = viewModel::setHideSettingsButton,
        onSetHomeComponentAlwaysEnabled = viewModel::setHomeComponentAlwaysEnabled,
        onSetScreenOrientation = viewModel::setScreenOrientation,
        onSetShowBatteryIndicator = viewModel::setShowBatteryIndicator,
        closeSettings = closeSettings
    )
}

@Composable
fun SettingsLockdown(
    viewState : SettingsLockdownViewModel.ViewState?,
    onSetFullKioskMode: (setting: SettingsLockdownViewModel.FullKioskModeSetValue) -> Unit,
    onOpenKioskModeSetup: () -> Unit,
    onOpenLayoutSettings: () -> Unit,
    onSetHideSettingsButton: (hide: Boolean) -> Unit,
    onSetHomeComponentAlwaysEnabled: (alwaysEnabled: Boolean) -> Unit,
    onSetScreenOrientation: (newValue: ScreenOrientation) -> Unit,
    onSetShowBatteryIndicator: (show: Boolean) -> Unit,
    closeSettings: () -> Unit,
) {
    var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsLockdownDialogType?>(null) }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        val settingItemModifier = Modifier.Companion.defaultSettingsItem()
        Text(
            stringResource(R.string.settings_ui_lockdown_settings_description),
            modifier = Modifier
                .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
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
                icon = Icons.Default.LockOpen,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_orientation_item),
                summary = stringResource(viewState.screenOrientation.labelRes()),
                onClick = { showUiModeDialog = SettingsLockdownDialogType.ScreenOrientation },
                icon = Icons.Default.ScreenLockRotation,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_layout_settings_item),
                onClick = onOpenLayoutSettings,
                icon = Icons.Default.FitScreen,
                modifier = settingItemModifier,
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_lockdown_show_battery),
                value = viewState.showBattery,
                onChange = onSetShowBatteryIndicator,
                icon = Icons.Default.BatteryStd,
                modifier = settingItemModifier,
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
                icon = painterResource(R.drawable.icon_settings_strikethrough),
                modifier = settingItemModifier
            )

            SectionTitle(
                R.string.settings_ui_section_advanced,
                Modifier.padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_lockdown_home_component_item),
                summary = homeComponentAlwaysEnabledLabel(viewState.homeComponentAlwaysEnabled),
                onClick = { showUiModeDialog = SettingsLockdownDialogType.HomeComponentMode },
                icon = Icons.Default.PhonelinkLock,
                modifier = settingItemModifier
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
                SettingsLockdownDialogType.HomeComponentMode -> HomeComponentModeDialog(
                    value = viewState.homeComponentAlwaysEnabled,
                    onValueChange = onSetHomeComponentAlwaysEnabled,
                    onDismissRequest = dismissAction
                )
                SettingsLockdownDialogType.ScreenOrientation -> ScreenOrientationDialog(
                    value = viewState.screenOrientation,
                    onValueChange = onSetScreenOrientation,
                    onDismissRequest = dismissAction
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun HomeComponentModeDialog(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectFromRadioListDialog(
        title = stringResource(R.string.settings_ui_lockdown_home_component_item),
        selectedValue = value,
        values = listOf(false, true),
        produceLabel = @Composable { homeComponentAlwaysEnabledLabel(it) },
        produceSummary = @Composable {
            stringResource(
                if (it) R.string.settings_ui_lockdown_home_component_always_description
                else R.string.settings_ui_lockdown_home_component_kiosk_only_description
            )
        },
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    )
}


@Composable
private fun KioskModeSelectionDialog(
    onValueChange: (SettingsLockdownViewModel.FullKioskModeSetValue) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectFromListDialog(
        title = stringResource(R.string.settings_ui_lockdown_kiosk_mode_dialog_title),
        message = stringResource(R.string.settings_ui_lockdown_kiosk_mode_dialog_description),
        values = listOf(
            SettingsLockdownViewModel.FullKioskModeSetValue.Enable,
            SettingsLockdownViewModel.FullKioskModeSetValue.Disable,
            SettingsLockdownViewModel.FullKioskModeSetValue.DisableTemporarily
        ),
        produceLabel = @Composable { value ->
            val stringRes = when (value) {
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

@Composable
private fun homeComponentAlwaysEnabledLabel(alwaysEnabled: Boolean): String =
    stringResource(
        if (alwaysEnabled) R.string.settings_ui_lockdown_home_component_always
        else R.string.settings_ui_lockdown_home_component_kiosk_only
    )

@Composable
private fun IsFullKioskEnabled.Value.toSummary(): String {
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
private fun ScreenOrientationDialog(
    value: ScreenOrientation,
    onValueChange: (ScreenOrientation) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SelectFromRadioListDialog(
        selectedValue = value,
        values = listOf(ScreenOrientation.AUTO, ScreenOrientation.PORTRAIT, ScreenOrientation.LANDSCAPE_AUTO,
            ScreenOrientation.LANDSCAPE, ScreenOrientation.LANDSCAPE_REVERSE),
        produceLabel = { stringResource(id = it.labelRes()) },
        title = stringResource(R.string.settings_ui_orientation_item),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest,
    )
}

@StringRes
private fun ScreenOrientation.labelRes(): Int = when(this) {
    ScreenOrientation.AUTO -> R.string.settings_ui_orientation_auto
    ScreenOrientation.PORTRAIT -> R.string.settings_ui_orientation_portrait
    ScreenOrientation.LANDSCAPE_AUTO -> R.string.settings_ui_orientation_landscape_auto
    ScreenOrientation.LANDSCAPE -> R.string.settings_ui_orientation_landscape_locked
    ScreenOrientation.LANDSCAPE_REVERSE -> R.string.settings_ui_orientation_landscape_locked_reverse
}

@Preview
@Composable
private fun SettingsLockdownPreview() {
    HomerPlayer2Theme {
        val viewState = SettingsLockdownViewModel.ViewState(
            fullKioskMode = IsFullKioskEnabled.Disabled,
            fullKioskModeAvailable = true,
            hideSettingsButton = false,
            homeComponentAlwaysEnabled = false,
            screenOrientation = ScreenOrientation.AUTO,
            showBattery = true
        )
        SettingsLockdown(
            viewState,
            {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

