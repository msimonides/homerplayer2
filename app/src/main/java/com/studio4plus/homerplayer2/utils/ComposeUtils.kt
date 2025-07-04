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

package com.studio4plus.homerplayer2.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.max

fun max(a: PaddingValues, b: PaddingValues): PaddingValues = object : PaddingValues {
    override fun calculateBottomPadding(): Dp =
        max(a.calculateBottomPadding(), b.calculateBottomPadding())

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        max(a.calculateLeftPadding(layoutDirection), b.calculateLeftPadding(layoutDirection))

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        max(a.calculateRightPadding(layoutDirection), b.calculateRightPadding(layoutDirection))

    override fun calculateTopPadding(): Dp =
        max(a.calculateTopPadding(), b.calculateTopPadding())
}

fun Modifier.blockTouchEvents() = pointerInput("block events") {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .forEach(PointerInputChange::consume)
        }
    }
}

fun Modifier.optional(include: Boolean, modifier: Modifier.() -> Modifier): Modifier =
    if (include) this.then(this.modifier()) else this
