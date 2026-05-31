/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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

package com.studio4plus.homerplayer2.base.ui.theme


import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.unveilIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

@OptIn(ExperimentalAnimationApi::class)
fun <T : Any> unveilPredictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    { edge ->
        val directionSign = if (edge == NavigationEvent.EDGE_LEFT) 1 else -1
        ContentTransform(
            unveilIn(
                spring(
                    dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                )
            ),
            scaleOut(targetScale = 0.7f) +
                    fadeOut(targetAlpha = 0.95f) +
                    slideOutHorizontally { fullWidth -> directionSign * (fullWidth * 0.07f).fastRoundToInt() },
        )
    }

@Suppress("UNCHECKED_CAST")
fun unveilPredictivePopTransitionSpecAny(): AnimatedContentTransitionScope<Scene<*>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    unveilPredictivePopTransitionSpec<NavKey>() as AnimatedContentTransitionScope<Scene<*>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform