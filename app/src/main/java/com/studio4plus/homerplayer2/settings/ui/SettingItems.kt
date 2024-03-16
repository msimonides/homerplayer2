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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme

@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingRow(
        modifier = Modifier
            .toggleable(
                value = value,
                onValueChange = onChange,
                role = Role.Switch
            )
            .then(modifier),
        summary = summary,
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(HomerTheme.dimensions.labelSpacing))
            Switch(checked = value, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    SettingRow(
        modifier = Modifier
            .then(click)
            .then(modifier),
        summary = summary,
    ) {
        Text(label, modifier = Modifier.padding(bottom = if (summary != null) 4.dp else 0.dp))
    }
}

@Composable
private fun SettingRow(
    modifier: Modifier = Modifier,
    summary: String? = null,
    labelRow: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        labelRow()
        if (summary != null) {
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun SettingItemPreview() {
    HomerPlayer2Theme {
        SettingItem("Some setting", summary = "Enabled", onClick = {})
    }
}

@Preview
@Composable
private fun SettingSwitchWithSummaryPreview() {
    HomerPlayer2Theme {
        SettingSwitch("Some switch", summary = "Description", value = false, onChange = {})
    }
}

@Preview
@Composable
private fun SettingSwitchWithoutSummaryPreview() {
    HomerPlayer2Theme {
        SettingSwitch("Some switch", value = false, onChange = {})
    }
}