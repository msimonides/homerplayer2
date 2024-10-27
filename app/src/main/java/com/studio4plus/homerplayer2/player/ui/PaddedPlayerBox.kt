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

package com.studio4plus.homerplayer2.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.settingsdata.PlayerLayoutMargins
import com.studio4plus.homerplayer2.settingsdata.PlayerLayoutSettings

const val MAX_PLAYER_MARGIN_HORIZ_FRACTION = 0.2f
const val MAX_PLAYER_MARGIN_BOTTOM_FRACTION = 0.25f

@Composable
fun PaddedPlayerBox(
    layoutSettings: PlayerLayoutSettings,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    maxHorizontalPaddingFraction: Float = MAX_PLAYER_MARGIN_HORIZ_FRACTION,
    maxBottomPaddingFraction: Float = MAX_PLAYER_MARGIN_BOTTOM_FRACTION,
    contentBackgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit,
) {
    val paddings = when {
        isLandscape -> layoutSettings.landscapeMargins
        else -> layoutSettings.portraitMargins
    }
    BoxWithConstraints(modifier = modifier) {
        val maxHorizontalPadding = this.maxWidth * maxHorizontalPaddingFraction
        val maxBottomPadding = this.maxHeight * maxBottomPaddingFraction
        val horizontal = paddings.horizontalDp.coerceIn(0.dp, maxHorizontalPadding)
        val bottom = paddings.bottomDp.coerceIn(0.dp, maxBottomPadding)
        Box(
            modifier = Modifier
                .padding(start = horizontal, end = horizontal, bottom = bottom)
                .background(contentBackgroundColor),
            content = content
        )
    }
}

private val PlayerLayoutMargins.horizontalDp get() = this.horizontal.dp
private val PlayerLayoutMargins.bottomDp get() = this.bottom.dp