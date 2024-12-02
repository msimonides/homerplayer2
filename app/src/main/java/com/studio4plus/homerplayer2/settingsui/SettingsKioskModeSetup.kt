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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.Constants
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
fun SettingsKioskModeSetupRoute() = SettingsKioskModeSetup()

@Composable
fun SettingsKioskModeSetup() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
            .navigationBarsPadding()
    ) {
        val context = LocalContext.current
        Text(
            text = stringResource(R.string.settings_ui_kiosk_mode_setup_description1),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BulletText(text = stringResource(R.string.settings_ui_kiosk_mode_setup_description1_bullet1))
        BulletText(text = stringResource(R.string.settings_ui_kiosk_mode_setup_description1_bullet2))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.settings_ui_kiosk_mode_setup_description2))

        Button(
            onClick = { openSetupInstructions(context) },
            modifier = Modifier
                .padding(top = 24.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.settings_ui_kiosk_mode_setup_open_instructions))
        }
        Text(
            stringResource(BaseR.string.generic_website_alternative, Constants.UrlKioskSetupInstructions),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun BulletText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .padding(bottom = 4.dp),
    ) {
        Text("•", style = style, modifier = Modifier.padding(end  = 4.dp))
        Text(text, style = style)
    }
}

private fun openSetupInstructions(context: Context) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(Constants.UrlKioskSetupInstructions))
    )
}

@Preview
@Composable
private fun PreviewSettingsKioskModeSetup() {
    HomerPlayer2Theme {
        SettingsKioskModeSetup()
    }
}
