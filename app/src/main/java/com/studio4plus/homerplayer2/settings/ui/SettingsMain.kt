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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.settings.UiThemeMode
import com.studio4plus.homerplayer2.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel


@Composable
fun SettingsMain(
    viewModel: MainViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        var showUiModeDialog by remember { mutableStateOf(false) }
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
                onClick = { showUiModeDialog = true },
                modifier = settingItemModifier
            )
        }
        if (showUiModeDialog) {
            ChooseUiModeDialog(
                value = viewState.uiMode,
                onValueChange = {
                    viewModel.setUiMode(it)
                    showUiModeDialog = false
                },
                onDismissRequest = { showUiModeDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseUiModeDialog(
    value: UiThemeMode,
    onValueChange: (UiThemeMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Surface(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    stringResource(id = R.string.settings_ui_mode_label),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                )
                arrayOf(UiThemeMode.SYSTEM, UiThemeMode.LIGHT, UiThemeMode.DARK).forEach { uiMode ->
                    RadioWithLabel(
                        label = stringResource(uiMode.labelRes()),
                        selected = value == uiMode,
                        onClick = { onValueChange(uiMode) },
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

private fun UiThemeMode.labelRes() = when(this) {
    UiThemeMode.SYSTEM -> R.string.settings_ui_system
    UiThemeMode.LIGHT -> R.string.settings_ui_light
    UiThemeMode.DARK -> R.string.settings_ui_dark
}
