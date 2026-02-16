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

package com.studio4plus.homerplayer2.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.studio4plus.homerplayer2.utils.Clock
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.serialization.KSerializer

const val BACK_NAVIGATION_DELAY_MS = 300

@Composable
fun rememberNavBackStackState(
    clock: Clock,
    initialDestination: NavKey,
    navBackStackSerializer: KSerializer<NavBackStack<NavKey>>,
    configuration: SavedStateConfiguration,
): NavBackStackState<NavKey> =
    rememberSaveable(
        initialDestination,
        saver = NavBackStackState.saver(clock, navBackStackSerializer, configuration),
    ) {
        NavBackStackState(NavBackStack(initialDestination), clock)
    }

// Note: consider moving to base.
@OptIn(ExperimentalAtomicApi::class)
class NavBackStackState<T : NavKey>(val navBackStack: NavBackStack<T>, private val clock: Clock) {
    private var lastRemoveTime: Long = 0

    fun goBack() {
        goBackIfPossible { navBackStack.removeLastOrNull() }
    }

    fun goBackUntil(predicate: (T) -> Boolean) {
        goBackIfPossible {
            while (!predicate(navBackStack.last()) && navBackStack.isNotEmpty()) {
                navBackStack.removeLastOrNull()
            }
        }
    }

    fun clear() {
        navBackStack.clear()
    }

    fun go(destination: T) {
        navBackStack.add(destination)
    }

    private fun goBackIfPossible(stackAction: () -> Unit) {
        val now = clock.elapsedRealTime()
        val canGoBack = now - lastRemoveTime > BACK_NAVIGATION_DELAY_MS
        if (canGoBack) {
            stackAction()
            lastRemoveTime = now
        }
    }

    companion object {
        fun saver(
            clock: Clock,
            navStackSerializer: KSerializer<NavBackStack<NavKey>>,
            navStackSerializerConfiguration: SavedStateConfiguration,
        ) =
            Saver<NavBackStackState<NavKey>, android.os.Bundle>(
                save = { original ->
                    encodeToSavedState(
                        navStackSerializer,
                        original.navBackStack,
                        navStackSerializerConfiguration,
                    )
                },
                restore = { savedState ->
                    NavBackStackState(
                        decodeFromSavedState(
                            navStackSerializer,
                            savedState,
                            navStackSerializerConfiguration,
                        ),
                        clock,
                    )
                },
            )
    }
}
