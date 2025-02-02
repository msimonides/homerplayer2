/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PlayerButtonIndicationFactory(
    private val largeEffect: Boolean,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PlayerButtonIndication(interactionSource, largeEffect)
}

private class PlayerButtonIndication(
    private val interactionSource: InteractionSource,
    largeEffect: Boolean,
) : Modifier.Node(), DrawModifierNode {

    private val animatedScalePercent = Animatable(1f)

    private val pressedScale = if (largeEffect) 1.2f else 1.1f
    private val clickedScale = if (largeEffect) 1.5f else 1.15f

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest {
                when (it) {
                    is PressInteraction.Press -> animateTo(pressedScale)
                    is PressInteraction.Cancel -> animateTo(1.0f)
                    is PressInteraction.Release -> {
                        animateTo(clickedScale)
                        animateTo(1.0f)
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        scale(animatedScalePercent.value) {
            this@draw.drawContent()
        }
    }

    private suspend fun animateTo(targetScale: Float) {
        animatedScalePercent.animateTo(targetScale)
    }
}
