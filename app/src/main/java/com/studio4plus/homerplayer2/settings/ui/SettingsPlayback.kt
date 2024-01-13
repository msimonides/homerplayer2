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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.TimeUnit

@Composable
fun SettingsPlaybackRoute(
    viewModel: SettingsPlaybackViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        SettingsPlayback(
            viewState = viewState,
            onSetRewindOnResumeSeconds = viewModel::setRewindOnResumeSeconds,
            onSetSleepTimerSeconds = viewModel::setSleepTimerSeconds,
        )
    }
}

private enum class SettingsMainDialogType {
    PlaybackRewindOnResume, SleepTimer
}

@Composable
fun SettingsPlayback(
    viewState: SettingsPlaybackViewModel.ViewState,
    onSetRewindOnResumeSeconds: (Int) -> Unit,
    onSetSleepTimerSeconds: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUiModeDialog by rememberSaveable { mutableStateOf<SettingsMainDialogType?>(null) }
    val settingItemModifier = Modifier.defaultSettingsItem()
    Column(modifier = modifier) {
        SettingItem(
            label = stringResource(id = R.string.settings_ui_playback_rewind_on_resume_label),
            summary = rewindOnResumeSettingString(seconds = viewState.rewindOnResumeSeconds),
            onClick = { showUiModeDialog = SettingsMainDialogType.PlaybackRewindOnResume },
            modifier = settingItemModifier
        )
        SettingItem(
            label = stringResource(id = R.string.settings_ui_playback_sleep_timer_label),
            summary = sleepTimerSettingString(seconds = viewState.sleepTimerSeconds),
            onClick = { showUiModeDialog = SettingsMainDialogType.SleepTimer },
            modifier = settingItemModifier
        )
    }

    val dismissAction = { showUiModeDialog = null }
    when (showUiModeDialog) {
        SettingsMainDialogType.PlaybackRewindOnResume -> SelectRewindOnResumeDialog(
            value = viewState.rewindOnResumeSeconds,
            onValueChange = onSetRewindOnResumeSeconds,
            onDismissRequest = dismissAction,
        )
        SettingsMainDialogType.SleepTimer -> SelectSleepTimerDialog(
            value = viewState.sleepTimerSeconds,
            onValueChange = onSetSleepTimerSeconds,
            onDismissRequest = dismissAction,
        )
        null -> Unit
    }
}

@Composable
private fun SelectRewindOnResumeDialog(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    SelectFromListDialog(
        selectedValue = value,
        values = listOf(0, 5, 15, 30, 60),
        produceLabel = { rewindOnResumeSettingString(it) },
        title = stringResource(id = R.string.settings_ui_playback_rewind_on_resume_label),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun rewindOnResumeSettingString(seconds: Int): String =
    when (seconds) {
        0 -> stringResource(id = R.string.settings_ui_playback_rewind_on_resume_setting_disabled)
        else -> pluralStringResource(id = R.plurals.settings_time_seconds, seconds, seconds)
    }

@Composable
private fun SelectSleepTimerDialog(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    SelectFromListDialog(
        selectedValue = value,
        values = listOf(
            0,
            30,
            TimeUnit.MINUTES.toSeconds(5).toInt(),
            TimeUnit.MINUTES.toSeconds(15).toInt(),
            TimeUnit.MINUTES.toSeconds(30).toInt(),
        ),
        produceLabel = { sleepTimerSettingString(it) },
        title = stringResource(id = R.string.settings_ui_playback_sleep_timer_label),
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun sleepTimerSettingString(seconds: Int): String =
    when {
        seconds == 0 -> stringResource(id = R.string.settings_ui_playback_sleep_timer_disabled)
        seconds < 60 -> pluralStringResource(id = R.plurals.settings_time_seconds, seconds, seconds)
        else -> {
            val minutes = seconds / 60
            pluralStringResource(id = R.plurals.settings_time_minutes, minutes, minutes)
        }
    }
