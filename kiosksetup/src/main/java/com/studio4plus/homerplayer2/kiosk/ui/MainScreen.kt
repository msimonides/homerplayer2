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

package com.studio4plus.homerplayer2.kiosk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.kiosk.R
import org.koin.androidx.compose.koinViewModel
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
fun MainScreenRoute(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle(null).value
    if (viewState != null) {
        MainScreen(
            viewState = viewState,
            dropDeviceOwnerPrivilege = viewModel::dropDeviceOwnerPrivilege,
        )
    }
}

@Composable
fun MainScreen(
    viewState: MainScreenViewState,
    dropDeviceOwnerPrivilege: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = HomerTheme.dimensions.screenContentPadding)
    ) {
        Image(
            painterResource(BaseR.drawable.app_icon_setup_foreground),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .clip(RectangleShape)
                .paint(
                    painterResource(BaseR.drawable.app_icon_background),
                    contentScale = ContentScale.Crop,
                )
                .height(256.dp)
                .fillMaxWidth()
        )
        val rowModifier = Modifier
            .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
        val textRowModifier = rowModifier.fillMaxWidth()

        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = rowModifier.padding(top = 24.dp, bottom = 16.dp)
        )
        Text(
            stringResource(viewState.statusTitle),
            style = MaterialTheme.typography.titleMedium,
            modifier = textRowModifier.padding(bottom = 4.dp)
        )
        Text(
            stringResource(viewState.statusDescription),
            modifier = textRowModifier
        )

        val context = LocalContext.current
        Button(
            onClick = { context.startActivity(viewState.mainActionIntent) },
            modifier = rowModifier.padding(top = 24.dp)
        ) {
            Text(
                stringResource(id = viewState.mainActionLabel)
            )
        }
        if (viewState.mainActionWebsiteUrl != null) {
            Text(
                stringResource(R.string.instructions_website_alternative, viewState.mainActionWebsiteUrl),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = textRowModifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier
            .weight(1f)
            .heightIn(min = 32.dp))
        if (viewState.dropPrivilegeEnabled) {
            var confirmationDialog by rememberSaveable() { mutableStateOf(false) }
            TextButton(
                onClick = { confirmationDialog = true },
                modifier = rowModifier
            ) {
                Text("Drop device owner privilege")
            }
            if (confirmationDialog) {
                ConfirmDropPrivilegeDialog(
                    onConfirm = dropDeviceOwnerPrivilege,
                    onDismissRequest = { confirmationDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ConfirmDropPrivilegeDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = modifier
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.drop_privilege_confirm_message),
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.drop_privilege_confirm_cancel_button))
                }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.drop_privilege_confirm_confirm_button))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewConfirmDropPrivilegeDialog() {
    HomerPlayer2Theme {
        ConfirmDropPrivilegeDialog(onConfirm = {}, onDismissRequest = {})
    }
 }

