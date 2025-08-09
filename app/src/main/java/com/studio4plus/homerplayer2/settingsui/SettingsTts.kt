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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.player.usecases.newBookTitleTtsPhrase
import com.studio4plus.homerplayer2.settingsui.composables.SettingCheckbox
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import com.studio4plus.homerplayer2.speech.SpeechTestViewModel
import com.studio4plus.homerplayer2.speech.TtsCheckContract
import org.koin.androidx.compose.koinViewModel
import com.studio4plus.homerplayer2.base.R as BaseR

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
            onOpenTtsSettings = { speechTestViewModel.openTtsSettings(context) },
            onSetAnnounceNewSettings = viewModel::setAnnounceNewTitles,
            onTestAnnouncePhrase = { new ->
                settingsViewState.selectedBookTitle?.let { bookTitle ->
                    speechTestViewModel.say(newBookTitleTtsPhrase(new, bookTitle))
                }
            }
        )
    }
}

private enum class SettingsTtsDialogType {
    AnnounceNewSettings,
}

@Composable
private fun SettingsTtsScreen(
    settingsViewState: SettingsTtsViewModel.ViewState,
    speechTestViewState: SpeechTestViewModel.ViewState,
    onSetReadBookTitles: (Boolean) -> Unit,
    onPlayTestPhrase: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    onSetAnnounceNewSettings: (enabled: Boolean, phrase: String?) -> Unit,
    onTestAnnouncePhrase: (phrase: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf<SettingsTtsDialogType?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        val settingItemModifier = Modifier.defaultSettingsItem()
        SettingSwitch(
            label = stringResource(id = R.string.settings_ui_tts_read_book_titles),
            value = settingsViewState.readBookTitlesEnabled,
            onChange = onSetReadBookTitles,
            icon = Icons.Default.RecordVoiceOver,
            modifier = settingItemModifier,
        )
        AnimatedVisibility(settingsViewState.readBookTitlesEnabled) {
            val announceNewSetting = if (settingsViewState.readBookTitlesAnnounceNew) {
                R.string.settings_ui_tts_read_book_titles_announce_new_enabled
            } else {
                R.string.settings_ui_tts_read_book_titles_announce_new_disabled
            }
            val announceNewPhrase =
                settingsViewState.readBookTitlesAnnounceNewPhrase ?: stringResource(R.string.content_tts_new_title)
            SettingItem(
                label = stringResource(id = R.string.settings_ui_tts_read_book_titles_announce_new_item, announceNewPhrase),
                onClick = { showDialog = SettingsTtsDialogType.AnnounceNewSettings },
                summary = stringResource(announceNewSetting),
                icon = Icons.Outlined.Info,
                modifier = settingItemModifier,
            )
        }
        SettingItem(
            label = stringResource(R.string.speech_play_test_phrase),
            onClick = onPlayTestPhrase,
            icon = Icons.Default.PlayArrow,
            modifier = settingItemModifier,
        )
        if (speechTestViewState.showTtsSettings) {
            SettingItem(
                label = stringResource(R.string.settings_ui_tts_open_settings),
                onClick = onOpenTtsSettings,
                icon = Icons.Default.Settings,
                modifier = settingItemModifier,
            )
        }
    }
    when (showDialog) {
        SettingsTtsDialogType.AnnounceNewSettings -> {
            DialogAnnounceNewTitles(
                announceNewEnabled = settingsViewState.readBookTitlesAnnounceNew,
                announceNewPhrase = settingsViewState.readBookTitlesAnnounceNewPhrase,
                onSaveChanges = onSetAnnounceNewSettings,
                onSayPhrase = onTestAnnouncePhrase,
                onDismiss = { showDialog = null },
            )
        }
        null -> {}
    }
}

@Composable
private fun DialogAnnounceNewTitles(
    announceNewEnabled: Boolean,
    announceNewPhrase: String?,
    onSaveChanges: (enable: Boolean, phrase: String?) -> Unit,
    onSayPhrase: ((phrase: String) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultPhrase = stringResource(R.string.content_tts_new_title)

    var isPhraseEnabled by rememberSaveable { mutableStateOf(announceNewEnabled) }
    var phrase by rememberSaveable { mutableStateOf(announceNewPhrase ?: defaultPhrase) }

    DefaultAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        buttons = {
            if (onSayPhrase != null) {
                TextButton(
                    enabled = isPhraseEnabled,
                    onClick = { onSayPhrase(phrase) }
                ) {
                    Text(stringResource(R.string.settings_ui_tts_read_book_titles_announce_new_test))
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            TextButton(onClick = onDismiss) {
                Text(stringResource(BaseR.string.generic_dialog_cancel))
            }

            TextButton(
                onClick = {
                    onSaveChanges(
                        isPhraseEnabled,
                        // It would be best if there was a way to display the placeholder always
                        // (even when not focused) and use that for the default phrase.
                        phrase.takeIf { it != defaultPhrase }
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(BaseR.string.generic_dialog_confirm))
            }
        }
    ) { horizontalPadding ->

        Text(
            stringResource(id = R.string.settings_ui_tts_read_book_titles_announce_new_descripton),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp, start = horizontalPadding, end = horizontalPadding)
        )

        SettingCheckbox(
            label = stringResource(R.string.settings_ui_tts_read_book_titles_announce_new_switch, phrase),
            icon = null,
            value = isPhraseEnabled,
            onChange = { isPhraseEnabled = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .heightIn(min = HomerTheme.dimensions.settingsRowMinHeight)
        )

        OutlinedTextField(
            enabled = isPhraseEnabled,
            value = phrase,
            label = { Text("Phrase") },
            onValueChange = { phrase = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
        )
    }
}

@Preview
@Composable
private fun PreviewDialogAnnounceNewTitles() {
    HomerPlayer2Theme {
        DialogAnnounceNewTitles(
            announceNewEnabled = true,
            announceNewPhrase = null,
            onSaveChanges = { _, _ -> },
            onSayPhrase = {},
            onDismiss = {}
        )
    }
}