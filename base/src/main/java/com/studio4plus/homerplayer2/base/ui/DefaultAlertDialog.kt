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

package com.studio4plus.homerplayer2.base.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicDefaultAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    usePlatformDefaultWidth: Boolean = true,
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(vertical = 24.dp, horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth),
    ) {
        Surface(
            Modifier.wrapContentSize(),
            shape = MaterialTheme.shapes.large,
            content = content
        )
    }
}

@Composable
fun DefaultAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    usePlatformDefaultWidth: Boolean = true,
    content: @Composable ColumnScope.(Dp) -> Unit,
) {
    BasicDefaultAlertDialog(
        onDismissRequest = onDismissRequest,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp)
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            }
            val scrollState = rememberScrollState()
            val topDividerAlpha by animateFloatAsState(targetValue = if (scrollState.canScrollBackward) 1f else 0f)
            val bottomDividerAlpha by animateFloatAsState(targetValue = if (scrollState.canScrollForward) 1f else 0f)
            HorizontalDivider(Modifier.graphicsLayer { alpha = topDividerAlpha })
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                content(24.dp)
            }
            HorizontalDivider(Modifier.graphicsLayer { alpha = bottomDividerAlpha })
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

@Preview
@Composable
private fun PreviewDialogWithTitleAndButtons() {
    DefaultAlertDialog(
        title = null,
        buttons = {
            TextButton(onClick = {}) { Text("Cancel") }
            TextButton(onClick = {}) { Text("Ok") }
        },
        content = { horizontalPadding ->
            Text( "Some message", modifier = Modifier.padding(horizontal = horizontalPadding))
        },
        onDismissRequest = {}
    )
}
