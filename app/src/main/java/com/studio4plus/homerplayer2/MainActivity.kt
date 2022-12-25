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
package com.studio4plus.homerplayer2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.browsing.BrowseScreen
import com.studio4plus.homerplayer2.onboarding.OnboardingAudiobookFoldersScreen
import com.studio4plus.homerplayer2.onboarding.OnboardingSpeechScreen
import com.studio4plus.homerplayer2.ui.theme.HomerPlayer2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomerPlayer2Theme {
                MainNavHost()
            }
        }
    }
}

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(modifier = modifier, navController = navController, startDestination = "onboarding/tts") {
        composable("onboarding/tts") {
            OnboardingSpeechScreen(navigateNext = { navController.navigate("onboarding/folders") })
        }
        composable("onboarding/folders") {
            OnboardingAudiobookFoldersScreen(
                navigateNext = { navController.navigate("books/browse") {
                    popUpTo("onboarding/tts") { inclusive = true }
                } },
                navigateBack = { navController.popBackStack() }
            )
        }
        composable("books/browse") {
            BrowseScreen()
        }
    }
}
