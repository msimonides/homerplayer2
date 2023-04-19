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

package com.studio4plus.homerplayer2.player.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.ui.theme.DefaultSpacing

private val settingsButtonSize = 48.dp
private val settingsIconSize = 32.dp

@Composable
fun BrowseBooks(
    books: List<PlayerViewModel.AudiobookState>,
    initialSelectedIndex: Int,
    onPlay: (bookIndex: Int) -> Unit,
    onPageChanged: (bookIndex: Int) -> Unit,
    onOpenSettings: () -> Unit,
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        BooksPager(
            landscape = landscape,
            modifier = Modifier.fillMaxSize(),
            itemPadding = DefaultSpacing.ScreenContentPadding,
            books = books,
            initialSelectedIndex = initialSelectedIndex,
            onPlay = onPlay,
            onPageChanged = onPageChanged
        )
        val settingsButtonModifier =
            Modifier.fillMaxWidth().padding(DefaultSpacing.ScreenContentPadding, 4.dp)
        SingleSettingsButton(
            onOpenSettings = onOpenSettings,
            modifier = if (landscape) {
                settingsButtonModifier
            } else {
                settingsButtonModifier.padding(end = ProgressIndicatorDefaults.width - (settingsButtonSize - settingsIconSize) / 2)
            }
        )
    }
}

@Composable
private fun SingleSettingsButton(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(settingsButtonSize)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                modifier = Modifier.size(settingsIconSize)
            )
        }
    }
}

@Composable
private fun DoubleSettingsButton(
    isVisible: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainSettingsButtonInteractionSource = remember { MutableInteractionSource() }
    val isMainSettingsButtonPressed = mainSettingsButtonInteractionSource.collectIsPressedAsState()
    Box(modifier) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(settingsButtonSize)
                .align(Alignment.TopEnd),
            interactionSource = mainSettingsButtonInteractionSource
        ) {
            if (isVisible) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                    modifier = Modifier.size(settingsIconSize)
                )
            }
        }
        if (isMainSettingsButtonPressed.value) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(settingsButtonSize)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                    modifier = Modifier.size(settingsIconSize)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewBrowseBooks() {
    val books = listOf(
        PlayerViewModel.AudiobookState("1", "Hamlet", 0.3f),
    )
    BrowseBooks(
        books = books,
        landscape = false,
        initialSelectedIndex = 0,
        onPlay = {},
        onPageChanged = {},
        onOpenSettings = {})
}
