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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.fullkioskmode.IsFullKioskEnabled
import com.studio4plus.homerplayer2.settingsdata.UiThemeMode
import com.studio4plus.homerplayer2.settingsui.composables.SelectFromRadioListDialog
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.settingsui.composables.SettingRowSummaryText
import com.studio4plus.homerplayer2.settingsui.usecases.ContentDescriptionFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
fun SettingsMainRoute(
    snackbarHostState: SnackbarHostState,
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
        onSetFullKioskMode = viewModel::setFullKioskMode,
        navigateFolders = navigateFolders,
        navigateLockdownSettings = navigateLockdownSettings,
        navigateNetworkSettings = navigateNetworkSettings,
        navigatePlaybackSettings = navigatePlaybackSettings,
        navigatePlayerUiSettings = navigatePlayerUiSettings,
        navigateTtsSettings = navigateTtsSettings,
        navigateAbout = navigateAbout,
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    val resources = LocalResources.current
    LaunchedEffect(Unit) {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", resources.configuration.locale)
        viewModel.eventFullKioskDisabledUntil
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { event ->
                snackbarHostState.showSnackbar(resources.getString(
                    R.string.settings_ui_lockdown_kiosk_mode_setting_disabled_snackbar,
                    timeFormatter.format(event.enableTime)
                ))
        }
    }
}

private enum class SettingsMainDialogType {
    UiMode
}

@Composable
private fun SettingsMain(
    viewState: SettingsMainViewModel.ViewState?,
    onSetUiMode: (UiThemeMode) -> Unit,
    onSetFullKioskMode: (Boolean) -> Unit,
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
                icon = R.drawable.icon_tune,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_playback_settings_item),
                onClick = navigatePlaybackSettings,
                icon = R.drawable.icon_play_arrow,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_content_item),
                summary = viewState.content.summary(),
                onClick = navigateFolders,
                icon = R.drawable.icon_library_music,
                modifier = settingItemModifier
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_network_item),
                onClick = navigateNetworkSettings,
                icon = R.drawable.icon_cell_wifi,
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
                icon = R.drawable.icon_record_voice_over,
                modifier = settingItemModifier,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_mode_label),
                summary = stringResource(viewState.uiMode.labelRes()),
                onClick = { showUiModeDialog = SettingsMainDialogType.UiMode },
                icon = R.drawable.icon_contrast,
                modifier = settingItemModifier
            )
            val lockdownEnabled = viewState.fullKioskMode == IsFullKioskEnabled.Enabled
            SettingItemWithSwitch(
                label = stringResource(R.string.settings_ui_lockdown_settings_item),
                onLabelClick = navigateLockdownSettings,
                summary = viewState.fullKioskMode.toSummary(),
                icon = if (lockdownEnabled) R.drawable.icon_lock else R.drawable.icon_lock_open,
                modifier = Modifier.heightIn(min = HomerTheme.dimensions.settingsRowMinHeight),
                onSwitchChange = onSetFullKioskMode.takeIf { viewState.showFullKioskSwitch },
                horizontalContentPadding = HomerTheme.dimensions.screenHorizPadding,
                verticalContentPadding = 8.dp,
                switchValue = lockdownEnabled,
            )
            SettingItem(
                label = stringResource(R.string.settings_ui_about_item),
                onClick = navigateAbout,
                icon = BaseR.drawable.icon_outlined_info,
                modifier = settingItemModifier
            )
            if (viewState.rateAppIntent != null) {
                val context = LocalContext.current
                SettingItem(
                    label = stringResource(R.string.settings_ui_rate_app_item),
                    onClick = { context.startActivity(viewState.rateAppIntent) },
                    icon = R.drawable.icon_family_star,
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

@Composable
private fun SettingItemWithSwitch(
    label: String,
    @DrawableRes icon: Int,
    onLabelClick: () -> Unit,
    onSwitchChange: ((Boolean) -> Unit)?,
    switchValue: Boolean,
    summary: String,
    verticalContentPadding: Dp,
    horizontalContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onLabelClick)
                .padding(
                    top = verticalContentPadding,
                    bottom = verticalContentPadding,
                    start = horizontalContentPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                Text(label)
                SettingRowSummaryText(summary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onSwitchChange != null) {
            VerticalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Switch(
                checked = switchValue,
                onCheckedChange = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .toggleable(value = switchValue, onValueChange = onSwitchChange)
                    .padding(horizontal = horizontalContentPadding)
            )
        }
    }
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
            fullKioskMode = IsFullKioskEnabled.Enabled,
            showFullKioskSwitch = true,
            rateAppIntent = null,
            ttsEnabled = true,
            uiMode = UiThemeMode.SYSTEM,
        )
        SettingsMain(viewState, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
