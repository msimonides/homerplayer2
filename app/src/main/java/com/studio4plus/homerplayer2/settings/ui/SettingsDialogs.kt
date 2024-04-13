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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog

@Composable
fun SettingsDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(Dp) -> Unit,
) {
    DefaultAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                content(24.dp)
            }
            if (buttons != null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    content = buttons
                )
            }
        }
    }
}

@Composable
fun <T> SelectFromRadioListDialog(
    selectedValue: T,
    values: List<T>,
    produceLabel: @Composable (T) -> String,
    title: String,
    onValueChange: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: (@Composable RowScope.() -> Unit)? = null,
) {
    SettingsDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        buttons = buttons,
    ) { horizontalPadding ->
        values.map { value ->
            RadioWithLabel(
                label = produceLabel(value),
                selected = value == selectedValue,
                onClick = {
                    onValueChange(value)
                    if (buttons == null) onDismissRequest()
                },
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
    SettingsDialog(
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

