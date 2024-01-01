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
package com.studio4plus.homerplayer2.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.onboarding.onboardingGraph
import com.studio4plus.homerplayer2.player.ui.PlayerScreen
import com.studio4plus.homerplayer2.settings.ui.SettingsScreen
import com.studio4plus.homerplayer2.core.ui.theme.HomerPlayer2Theme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val activityViewModel: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: show splash screen until state is Ready.

        setupLockTask()

        setContent {
            HomerPlayer2Theme {
                val viewState = activityViewModel.viewState.collectAsStateWithLifecycle().value

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewState) {
                        is MainActivityViewState.Loading -> Unit
                        is MainActivityViewState.Ready ->
                            MainNavHost(needsOnboarding = viewState.needsOnboarding)
                    }
                }
            }
        }
    }

    private fun setupLockTask() {
        val windowInsetsControllerCompat = WindowInsetsControllerCompat(window, window.decorView)
        activityViewModel.lockTask
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .onEach { isEnabled ->
                if (isEnabled) {
                    windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.statusBars())
                    startLockTask()
                } else {
                    windowInsetsControllerCompat.show(WindowInsetsCompat.Type.statusBars())
                    stopLockTask()
                }
            }
            .launchIn(lifecycleScope)
    }
}

@Composable
fun MainNavHost(
    needsOnboarding: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // TODO: use constants instead of raw strings
    val startDestination = if (needsOnboarding) "onboarding" else "player"
    NavHost(modifier = modifier, navController = navController, startDestination = startDestination) {
        onboardingGraph(navController, "player")
        composable("player") {
            PlayerScreen(onOpenSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
