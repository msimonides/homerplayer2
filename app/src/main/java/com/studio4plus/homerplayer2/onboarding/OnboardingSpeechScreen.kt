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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.SmallCircularProgressIndicator
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.speech.LaunchErrorSnackDisplay
import com.studio4plus.homerplayer2.speech.SpeechTestViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingSpeechRoute(
    navigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingSpeechViewModel = koinViewModel(),
    speechTestViewModel: SpeechTestViewModel = koinViewModel(),
) {
    val speechTestViewState by speechTestViewModel.viewState.collectAsStateWithLifecycle()

    val testPhrase = stringResource(id = R.string.speech_test_phrase)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(speechTestViewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(speechTestViewModel)
        }
    }

    LaunchErrorSnackDisplay(speechTestViewModel.errorEvent, snackbarHostState)

    val navigateNextAndFinish = {
        viewModel.onFinished()
        navigateNext()
    }
    val navigateNextAndConfirm = {
        viewModel.enableTts()
        navigateNextAndFinish()
    }
    OnboardingSpeechScreen(
        speechTestViewState = speechTestViewState,
        snackbarHostState = snackbarHostState,
        navigateNext = navigateNextAndConfirm,
        navigateSkip = navigateNextAndFinish,
        onSayTestPhrase = { speechTestViewModel.say(testPhrase) },
        onOpenTtsSettings = { speechTestViewModel.openTtsSettings(context) },
        modifier = modifier
    )
}

@Composable
fun OnboardingSpeechScreen(
    speechTestViewState: SpeechTestViewModel.ViewState,
    snackbarHostState: SnackbarHostState,
    navigateNext: () -> Unit,
    navigateSkip: () -> Unit,
    onSayTestPhrase: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = speechTestViewState.ttsTestSuccessful
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        val horizontalPaddingModifier =
            Modifier.padding(horizontal = HomerTheme.dimensions.screenHorizPadding)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(
                    horizontal = HomerTheme.dimensions.screenHorizExtraPadding,
                    vertical = HomerTheme.dimensions.screenVertPadding,
                ),
        ) {
            OnboardingHeader(
                titleRes = R.string.onboarding_speech_title,
                modifier = horizontalPaddingModifier
            )

            Text(
                stringResource(R.string.onboarding_speech_description),
                modifier = horizontalPaddingModifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(IntrinsicSize.Max)
                    .padding(bottom = 32.dp)
            ) {
                val buttonModifier = Modifier
                    .fillMaxWidth()
                if (canProceed) {
                    ButtonWithLoadingState(
                        onClick = onSayTestPhrase,
                        isFilled = false,
                        modifier = buttonModifier,
                    ) {
                        Text(stringResource(R.string.speech_play_test_phrase))
                    }

                    Button(
                        onClick = navigateNext,
                        modifier = buttonModifier,
                    ) {
                        Text(stringResource(R.string.onboarding_step_done))
                    }
                } else {
                    ButtonWithLoadingState(
                        onClick = onSayTestPhrase,
                        isLoading = speechTestViewState.isSpeaking,
                        modifier = buttonModifier,
                    ) {
                        Text(stringResource(R.string.speech_play_test_phrase))
                    }

                    OutlinedButton(
                        onClick = navigateSkip,
                        modifier = buttonModifier,
                    ) {
                        Text(stringResource(R.string.onboarding_speech_skip))
                    }
                }
            }

            if (speechTestViewState.ttsTested) {
                Text(
                    stringResource(R.string.onboarding_speech_open_settings_description),
                    modifier = horizontalPaddingModifier.padding(bottom = 16.dp)
                )

                OutlinedButton(
                    onClick = onOpenTtsSettings,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.onboarding_speech_open_settings))
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
    if (isFilled) Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = buttonContent
    )
    else OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = buttonContent
    )
}

@Preview
@Composable
private fun ScreenContentPreviewBeforeTest() {
    HomerPlayer2Theme {
        OnboardingSpeechScreen(
            speechTestViewState = SpeechTestViewModel.ViewState(
                showTtsSettings = true,
                isSpeaking = false,
                ttsTestSuccessful = false,
                ttsTested = false,
            ),
            snackbarHostState = SnackbarHostState(),
            navigateNext = {},
            navigateSkip = {},
            onSayTestPhrase = {},
            onOpenTtsSettings = {},
        )
    }
}

@Preview
@Composable
private fun ScreenContentPreviewAfterTest() {
    HomerPlayer2Theme {
        OnboardingSpeechScreen(
            speechTestViewState = SpeechTestViewModel.ViewState(
                showTtsSettings = true,
                isSpeaking = false,
                ttsTestSuccessful = true,
                ttsTested = true,
            ),
            snackbarHostState = SnackbarHostState(),
            navigateNext = {},
            navigateSkip = {},
            onSayTestPhrase = {},
            onOpenTtsSettings = {},
        )
    }
}