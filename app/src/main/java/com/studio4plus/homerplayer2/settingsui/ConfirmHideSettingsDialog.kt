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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.player.ui.DoubleSettingsButton
import kotlinx.coroutines.delay

@Composable
fun HideSettingsButtonConfirmationDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    DefaultAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        usePlatformDefaultWidth = false
    ) {
        HideSettingsButtonConfirmation(
            onConfirm = onConfirm,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun HideSettingsButtonConfirmation(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var successfullyTested by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(successfullyTested) {
        if (successfullyTested) {
            buttonVisible = false
        } else {
            while(true) {
                delay(1_000)
                buttonVisible = true
                delay(500)
                buttonVisible = false
                delay(1_000)
            }
        }
    }
    Column(
        modifier = modifier
    ) {
        HomerPlayer2Theme(
            darkTheme = false,
            largeScreen = false,
            setWindowColors = false
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    Image(
                        painterResource(id = R.drawable.phone_frame),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DoubleSettingsButton(
                        showButtons = buttonVisible,
                        onOpenSettings = { successfullyTested = true },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(BiasAlignment(0f, -0.25f))
                    )
                }
            }
        }
        Text(
            stringResource(R.string.settings_ui_lockdown_hide_settings_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            stringResource(R.string.settings_ui_lockdown_hide_settings_dialog_description),
        )
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
            enabled = successfullyTested
        ) {
            Text(stringResource(R.string.settings_ui_lockdown_hide_settings_dialog_confirm_button))
        }
    }
}

@Preview
@Composable
private fun PreviewConfirmHideSettings() {
    HomerPlayer2Theme {
        HideSettingsButtonConfirmation(onConfirm = {})
    }
}
