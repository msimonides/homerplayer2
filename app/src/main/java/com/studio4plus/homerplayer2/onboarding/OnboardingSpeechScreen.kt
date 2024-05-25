/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.SmallCircularProgressIndicator
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import com.studio4plus.homerplayer2.speech.SpeechTestViewModel
import com.studio4plus.homerplayer2.speech.TtsCheckContract
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingSpeechRoute(
    navigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingSpeechViewModel = koinViewModel(),
    speechTestViewModel: SpeechTestViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val speechTestViewState by speechTestViewModel.viewState.collectAsStateWithLifecycle()

    val testPhrase = stringResource(id = R.string.speech_test_phrase)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val snackbarHostState = remember { SnackbarHostState() }

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

    val navigateNextAndConfirm = {
        viewModel.confirmTtsChoice()
        navigateNext()
    }
    OnboardingSpeechScreen(
        viewState = viewState,
        speechTestViewState = speechTestViewState,
        snackbarHostState = snackbarHostState,
        navigateNext = navigateNextAndConfirm,
        navigateSkip = navigateNext,
        onSayTestPhrase = {
            speechTestViewModel.onTtsCheckStarted()
            ttsCheckLauncher.launch(Unit)
        },
        onTtsToggled = viewModel::onTtsToggled,
        onOpenTtsSettings = { speechTestViewModel.openTtsSettings(context) },
        modifier = modifier
    )
}

@Composable
fun OnboardingSpeechScreen(
    viewState: OnboardingSpeechViewModel.ViewState,
    speechTestViewState: SpeechTestViewModel.ViewState,
    snackbarHostState: SnackbarHostState,
    navigateNext: () -> Unit,
    navigateSkip: () -> Unit,
    onSayTestPhrase: () -> Unit,
    onTtsToggled: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = !viewState.readBookTitlesEnabled || speechTestViewState.ttsTestSuccessful
    Scaffold(
        modifier = modifier,
        bottomBar = {
            OnboardingNavigationButtons(
                nextEnabled = canProceed,
                nextLabel = R.string.onboarding_step_next,
                onNext = navigateNext,
                secondaryLabel = R.string.onboarding_step_skip,
                onSecondary = navigateSkip,
                modifier = Modifier.padding(OnboardingNavigationButtonsDefaults.paddingValues),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .padding(HomerTheme.dimensions.screenContentPadding),
            showTtsSettings = speechTestViewState.showTtsSettings,
            readBookTitlesEnabled = viewState.readBookTitlesEnabled,
            speechInProgress = speechTestViewState.isSpeaking,
            playTestPhraseIsCta = !canProceed,
            onSayTestPhrase = onSayTestPhrase,
            onTtsToggled = onTtsToggled,
            onOpenTtsSettings = onOpenTtsSettings,
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    showTtsSettings: Boolean,
    readBookTitlesEnabled: Boolean,
    speechInProgress: Boolean,
    playTestPhraseIsCta: Boolean,
    onSayTestPhrase: () -> Unit,
    onTtsToggled: () -> Unit,
    onOpenTtsSettings: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(id = R.string.onboarding_speech_description))

        Column(
            modifier = Modifier
                .align(CenterHorizontally)
                .width(IntrinsicSize.Max),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = readBookTitlesEnabled,
                    role = Role.Switch,
                    onValueChange = { onTtsToggled() }
                )
            ) {
                Switch(readBookTitlesEnabled, onCheckedChange = null)
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = stringResource(id = R.string.onboarding_speech_tts_checkbox_label))
                    Text(
                        style = MaterialTheme.typography.labelSmall,
                        text = stringResource(id = R.string.onboarding_speech_tts_checkbox_note)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            ButtonWithLoadingState(
                onClick = onSayTestPhrase,
                modifier = Modifier.fillMaxWidth(),
                isLoading = speechInProgress,
                isFilled = playTestPhraseIsCta,
                enabled = readBookTitlesEnabled
            ) { Text(text = stringResource(id = R.string.speech_play_test_phrase)) }
            if (showTtsSettings) {
                OutlinedButton(
                    onClick = onOpenTtsSettings,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = readBookTitlesEnabled
                ) {
                    Text(text = stringResource(id = R.string.onboarding_speech_open_settings))
                }
            }
        }
    }
}

@Composable
private fun ButtonWithLoadingState(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFilled: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val buttonContent: @Composable RowScope.() -> Unit = {
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing + ButtonDefaults.IconSize))
        content()
        if (isLoading) {
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            SmallCircularProgressIndicator()
        } else {
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing + ButtonDefaults.IconSize))
        }
    }
    if (isFilled) Button(onClick = onClick, modifier = modifier, enabled = enabled, content = buttonContent)
    else OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = buttonContent)
}

@Preview
@Composable
private fun ScreenContentPreview() {
    ScreenContent(
        showTtsSettings = true,
        readBookTitlesEnabled = true,
        speechInProgress = false,
        playTestPhraseIsCta = false,
        onSayTestPhrase = {},
        onTtsToggled = {},
        onOpenTtsSettings = {}
    )
}