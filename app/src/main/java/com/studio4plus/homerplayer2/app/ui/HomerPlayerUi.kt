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
import android.os.Build
import android.os.Vibrator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.base.ui.HomerHapticFeedback
import com.studio4plus.homerplayer2.base.ui.NoHapticFeedback
import com.studio4plus.homerplayer2.base.ui.VibratorProvider
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.onboarding.onboardingGraph
import com.studio4plus.homerplayer2.player.ui.PlayerRoute
import com.studio4plus.homerplayer2.settings.ui.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomerPLayerUi(viewModel: HomerPlayerUiVM = koinViewModel()) {
    HomerPlayer2Theme {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewState != null) {
                CompositionLocalProvider(
                    LocalHapticFeedback provides rememberHapticFeedback(viewState.hapticFeedbackEnabled)
                ) {
                    MainNavHost(
                        needsOnboarding = viewState.needsOnboarding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MainNavHost(
    needsOnboarding: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // TODO: use constants instead of raw strings
    val startDestination = if (needsOnboarding) "onboarding" else "player"
    NavHost(modifier = modifier, navController = navController, startDestination = startDestination) {
        onboardingGraph(navController, "player")
        composable("player") {
            PlayerRoute(onOpenSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            SettingsScreen(navigateBack = { navController.popBackStack("player", inclusive = false) })
        }
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
