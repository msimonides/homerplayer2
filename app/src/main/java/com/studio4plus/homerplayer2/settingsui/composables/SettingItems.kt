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

package com.studio4plus.homerplayer2.settingsui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
    icon: ImageVector,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingSwitch(label, value, rememberVectorPainter(icon), onChange, modifier, summary)
}

@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    icon: Painter?,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingRow(
        label = label,
        icon = icon,
        summary = summary,
        modifier = Modifier
            .toggleable(
                value = value,
                onValueChange = onChange,
                role = Role.Switch
            )
            .then(modifier),
    ) {
        Switch(checked = value, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
fun SettingRadio(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingRadio(label, selected, rememberVectorPainter(icon), onSelected, modifier, summary)
}

@Composable
fun SettingRadio(
    label: String,
    selected: Boolean,
    icon: Painter?,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingRow(
        label = label,
        modifier = Modifier
            .selectable(selected = selected, onClick = onSelected, role = Role.RadioButton)
            .then(modifier),
        summary = summary,
        icon = icon,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
fun SettingItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    summary: String? = null,
) {
    SettingItem(label, rememberVectorPainter(icon), modifier, onClick, summary)
}

@Composable
fun SettingItem(
    label: String,
    icon: Painter?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    summary: String? = null,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    SettingRow(
        label = label,
        modifier = Modifier
            .then(click)
            .then(modifier),
        icon = icon,
        summary = summary,
        controlItem = null,
    )
}

@Composable
private fun SettingRow(
    label: String,
    icon: Painter?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    controlItem: (@Composable () -> Unit)?,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                modifier = Modifier.padding(bottom = if (summary != null) 4.dp else 0.dp)
            )
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (controlItem != null) {
            Spacer(modifier = Modifier.width(HomerTheme.dimensions.labelSpacing))
            controlItem()
        }
    }
}

@Preview
@Composable
private fun SettingItemPreview() {
    HomerPlayer2Theme {
        SettingItem(
            label = "Some setting",
            icon = Icons.Default.Settings,
            summary = "Enabled",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun SettingSwitchWithSummaryPreview() {
    HomerPlayer2Theme {
        SettingSwitch(
            "Some switch",
            icon = Icons.Default.Settings,
            summary = "Description",
            value = false,
            onChange = {}
        )
    }
}

@Preview
@Composable
private fun SettingSwitchWithoutSummaryPreview() {
    HomerPlayer2Theme {
        SettingSwitch("Some switch", icon = Icons.Default.Settings, value = false, onChange = {})
    }
}