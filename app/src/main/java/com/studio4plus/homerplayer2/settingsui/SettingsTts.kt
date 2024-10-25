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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import com.studio4plus.homerplayer2.speech.SpeechTestViewModel
import com.studio4plus.homerplayer2.speech.TtsCheckContract
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsTtsRoute(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsTtsViewModel = koinViewModel(),
    speechTestViewModel: SpeechTestViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val testPhrase = stringResource(id = R.string.speech_test_phrase)

    val ttsCheckLauncher = rememberLauncherForActivityResult(
        contract = TtsCheckContract(),
        onResult = { success ->
            if (success) speechTestViewModel.say(testPhrase)
            else speechTestViewModel.onTtsCheckFailed()
        }
    )

    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(speechTestViewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(speechTestViewModel)
        }
    }

    LaunchErrorSnackDisplay(speechTestViewModel.errorEvent, snackbarHostState)

    val settingsViewState = viewModel.viewState.collectAsStateWithLifecycle().value
    val speechTestViewState by speechTestViewModel.viewState.collectAsStateWithLifecycle()

    if (settingsViewState != null) {
        SettingsTtsScreen(
            settingsViewState = settingsViewState,
            speechTestViewState = speechTestViewState,
            onSetReadBookTitles = viewModel::setReadBookTitles,
            onPlayTestPhrase = {
                speechTestViewModel.onTtsCheckStarted()
                ttsCheckLauncher.launch(Unit)
            },
            onOpenTtsSettings = { speechTestViewModel.openTtsSettings(context) }
        )
    }
}

@Composable
private fun SettingsTtsScreen(
    settingsViewState: SettingsTtsViewModel.ViewState,
    speechTestViewState: SpeechTestViewModel.ViewState,
    onSetReadBookTitles: (Boolean) -> Unit,
    onPlayTestPhrase: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        val settingItemModifier = Modifier.defaultSettingsItem()
        SettingSwitch(
            label = stringResource(id = R.string.settings_ui_tts_read_book_titles),
            value = settingsViewState.readBookTitlesEnabled,
            onChange = onSetReadBookTitles,
            modifier = settingItemModifier,
        )
        SettingItem(
            label = stringResource(R.string.speech_play_test_phrase),
            onClick = onPlayTestPhrase,
            modifier = settingItemModifier,
        )
        if (speechTestViewState.showTtsSettings) {
            SettingItem(
                label = stringResource(R.string.settings_ui_tts_open_settings),
                onClick = onOpenTtsSettings,
                modifier = settingItemModifier,
            )
        }
    }
}