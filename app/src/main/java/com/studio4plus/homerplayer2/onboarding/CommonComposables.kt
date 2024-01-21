/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme

object OnboardingNavigationButtonsDefaults {
    val paddingValues: PaddingValues
        @Composable
        get() = with(HomerTheme.dimensions) {
            PaddingValues(start = screenContentPadding, end = screenContentPadding, bottom = screenContentPadding)
        }
}

@Composable
fun OnboardingNavigationButtons(
    modifier: Modifier = Modifier,
    nextEnabled: Boolean,
    @StringRes nextLabel: Int,
    onNext: () -> Unit,
    @StringRes secondaryLabel: Int,
    onSecondary: () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onSecondary) {
            Text(text = stringResource(id = secondaryLabel))
        }
        Spacer(modifier = Modifier.width(HomerTheme.dimensions.labelSpacing))
        Button(onClick = onNext, enabled = nextEnabled) {
            Text(text = stringResource(id = nextLabel))
        }
    }
}