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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.speech.TtsCheckContract
import com.studio4plus.homerplayer2.ui.theme.HomerTheme
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSpeechScreen(
    modifier: Modifier = Modifier,
    navigateNext: () -> Unit
) {
    val viewModel: OnboardingSpeechViewModel = koinViewModel()
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()

    val testPhrase = stringResource(id = R.string.onboarding_speech_test_phrase)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentViewState = viewState.value
    val snackbarHostState = remember { SnackbarHostState() }

    val ttsCheckLauncher = rememberLauncherForActivityResult(
        contract = TtsCheckContract(),
        onResult = { success ->
            if (success) viewModel.say(testPhrase)
            else viewModel.onTtsCheckFailed()
        }
    )
    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    LaunchedEffect(viewModel.errorEvent) {
        viewModel.errorEvent.receiveAsFlow().collect { errorResId ->
            if (errorResId != null) {
                launch {
                    snackbarHostState.showSnackbar(
                        context.resources.getString(errorResId),
                        duration = SnackbarDuration.Long
                    )
                }
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            OnboardingNavigationButtons(
                nextEnabled = currentViewState.canProceed,
                nextLabel = R.string.onboarding_step_next,
                onNext = navigateNext,
                secondaryLabel = R.string.onboarding_step_skip,
                onSecondary = navigateNext,
                modifier = Modifier.padding(HomerTheme.dimensions.screenContentPadding)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .padding(HomerTheme.dimensions.screenContentPadding),
            showTtsSettings = currentViewState.showTtsSettings,
            readBookTitlesEnabled = currentViewState.readBookTitlesEnabled,
            speechInProgress = currentViewState.isSpeaking,
            playTestPhraseIsCta = !currentViewState.canProceed,
            onSayTestPhrase = {
                viewModel.onTtsCheckStarted()
                ttsCheckLauncher.launch(Unit)
            },
            onTtsToggled = viewModel::onTtsToggled,
            onOpenTtsSettings = { viewModel.openTtsSettings(context) },
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
            ) { Text(text = stringResource(id = R.string.onboarding_speech_play_test_phrase)) }
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
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                color = LocalContentColor.current,
                strokeWidth = 2.dp
            )
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