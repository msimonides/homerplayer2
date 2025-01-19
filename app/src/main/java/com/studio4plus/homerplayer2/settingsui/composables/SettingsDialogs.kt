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

package com.studio4plus.homerplayer2.settingsui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog

@Composable
fun <T> SelectFromRadioListDialog(
    selectedValue: T,
    values: List<T>,
    produceLabel: @Composable (T) -> String,
    title: String,
    onValueChange: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    produceSummary: (@Composable (T) -> String)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
) {
    DefaultAlertDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        buttons = buttons,
    ) { horizontalPadding ->
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = horizontalPadding, end = horizontalPadding)
            )
        }

        values.map { value ->
            SettingRadio(
                label = produceLabel(value),
                summary = produceSummary?.invoke(value),
                selected = value == selectedValue,
                onSelected = {
                    onValueChange(value)
                    if (buttons == null) onDismissRequest()
                },
                icon = null,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(vertical = 8.dp, horizontal = horizontalPadding)
            )
        }
    }
}

@Composable
fun <T> SelectFromListDialog(
    values: List<T>,
    produceLabel: @Composable (T) -> String,
    title: String,
    onValueChange: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultAlertDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) { horizontalPadding ->
        values.map { value ->
            Text(
                text = produceLabel(value),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onValueChange(value)
                        onDismissRequest()
                    }
                    .heightIn(min = 48.dp)
                    .padding(vertical = 8.dp, horizontal = horizontalPadding)
            )
        }
    }
}

