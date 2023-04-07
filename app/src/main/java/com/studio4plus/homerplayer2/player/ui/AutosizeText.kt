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

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A very primitive auto-size text.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun AutosizeText(
    text: String,
    modifier: Modifier = Modifier,
    minSize: TextUnit = 10.sp,
    maxSize: TextUnit = 112.sp,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
) {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val mergedStyle = style.copy(color = color)

    Layout(
        modifier = modifier
            .drawWithCache {
                onDrawBehind {
                    textLayoutResult.value?.let { drawText(it) }
                }
            }
            .semantics {
                this.text = AnnotatedString(text)
                getTextLayoutResult {
                    val layoutResult = textLayoutResult.value
                    if (layoutResult != null) {
                        it.add(layoutResult)
                        true
                    } else {
                        false
                    }
                }
            },
        measurePolicy = { _, constraints ->
            val textLayout = measureText(
                AnnotatedString(text),
                mergedStyle,
                textMeasurer,
                constraints,
                minSize,
                maxSize
            )
            textLayoutResult.value = textLayout
            layout(textLayout.size.width, textLayout.size.height) {}
        }
    )
}

@OptIn(ExperimentalTextApi::class)
fun measureText(
    annotatedText: AnnotatedString,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    constraints: Constraints,
    minSize: TextUnit,
    maxSize: TextUnit
): TextLayoutResult {
    var textSize = maxSize
    var textLayout = textMeasurer.measure(
        annotatedText,
        style = style.copy(fontSize = textSize, lineHeight = TextUnit.Unspecified),
        constraints = constraints
    )
    while (textSize > minSize && (textLayout.hasVisualOverflow || textLayout.breaksWords())) {
        textSize = textSize.times(0.9f) // TODO: better search algorithm
        textLayout = textMeasurer.measure(
            annotatedText,
            style = style.copy(fontSize = textSize, lineHeight = TextUnit.Unspecified),
            constraints = constraints
        )
    }
    return textLayout
}

private fun TextLayoutResult.breaksWords(): Boolean =
    // TODO: simplify when fixed: https://issuetracker.google.com/issues/270679576
    size.width < multiParagraph.intrinsics.minIntrinsicWidth

@Preview(widthDp = 100, heightDp = 60)
@Composable
fun AutosizeTextPreview() {
    AutosizeText("Abcd efg hijklm opqr stuvw xyz")
}