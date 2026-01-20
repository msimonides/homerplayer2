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

package com.studio4plus.homerplayer2.app.ui

import android.content.Context
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleOut
import androidx.compose.animation.unveilIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.studio4plus.homerplayer2.base.ui.HomerHapticFeedback
import com.studio4plus.homerplayer2.base.ui.NoHapticFeedback
import com.studio4plus.homerplayer2.base.ui.VibratorProvider
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.onboarding.OnboardingDestination
import com.studio4plus.homerplayer2.onboarding.onboardingEntries
import com.studio4plus.homerplayer2.player.ui.PlayerRoute
import com.studio4plus.homerplayer2.settingsui.nav.SettingsDestination
import com.studio4plus.homerplayer2.settingsui.nav.SettingsSceneStrategy
import com.studio4plus.homerplayer2.settingsui.nav.settingsEntries
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomerPLayerUi(
    eventNavigateToPlayer: SharedFlow<Unit>,
    viewModel: HomerPlayerUiVM = koinViewModel()
) {
    HomerPlayer2Theme {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (viewState != null) {
                val defaultDestination =
                    if (viewState.needsOnboarding) OnboardingDestination.Default else PlayerDestination
                val navBackStack = rememberNavBackStack(defaultDestination)

                if (!viewState.needsOnboarding) {
                    LaunchedEffect(eventNavigateToPlayer, navBackStack) {
                        eventNavigateToPlayer.collect {
                            navBackStack.clear()
                            navBackStack.add(PlayerDestination)
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalHapticFeedback provides
                        rememberHapticFeedback(viewState.hapticFeedbackEnabled)
                ) {
                    MainNavDisplay(
                        navBackStack = navBackStack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberNavBackStack(initialDestination: NavKey): NavBackStack<NavKey> =
    rememberSerializable(
        configuration =
            SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(baseClass = NavKey::class) {
                        OnboardingDestination.serializers()
                        SettingsDestination.serializers()
                        subclass(PlayerDestination::class, PlayerDestination.serializer())
                    }
                }
            },
        serializer = NavBackStackSerializer<NavKey>(),
    ) {
        NavBackStack(initialDestination)
    }

@Serializable object PlayerDestination : NavKey

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainNavDisplay(
    navBackStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    SharedTransitionLayout(modifier = modifier) {
        NavDisplay(
            backStack = navBackStack,
            onBack = { navBackStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            sceneStrategy = SettingsSceneStrategy(this@SharedTransitionLayout),
            entryProvider =
                entryProvider {
                    onboardingEntries(
                        navigate = { navBackStack.add(it) },
                        navigateBack = { navBackStack.removeLastOrNull() },
                        onFinished = {
                            navBackStack.clear()
                            navBackStack.add(PlayerDestination)
                        },
                    )
                    entry<PlayerDestination> {
                        PlayerRoute(
                            onOpenSettings = { navBackStack.add(SettingsDestination.Default) }
                        )
                    }
                    settingsEntries(
                        navBackStack = navBackStack,
                        snackbarHostState = snackbarHostState,
                    )
                },
            predictivePopTransitionSpec = {
                ContentTransform(
                    unveilIn(
                        spring(
                            dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                            stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                        )
                    ),
                    scaleOut(targetScale = 0.7f),
                )
            }
        )
    }
}

@Composable
private fun rememberHapticFeedback(isEnabled: Boolean): HapticFeedback {
    fun create(context: Context): HapticFeedback {
        val vibrator = if (isEnabled) VibratorProvider(context).get else null
        return vibrator?.let { HomerHapticFeedback(it) } ?: NoHapticFeedback()
    }

    val context = LocalContext.current
    return remember(isEnabled) { create(context) }
}
