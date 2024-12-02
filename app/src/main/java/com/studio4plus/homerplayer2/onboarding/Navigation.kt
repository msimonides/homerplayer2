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

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.studio4plus.homerplayer2.podcastsui.PodcastEditNav
import java.net.URLEncoder

fun NavGraphBuilder.onboardingGraph(navController: NavController, destinationRoute: String) {
    navigation("onboarding/folders", "onboarding") {
        composable("onboarding/folders") {
            OnboardingContentRoute(
                navigateAddPodcast = { navController.navigate("onboarding/podcast/") },
                navigateEditPodcast = { feedUri ->
                    val argument = URLEncoder.encode(feedUri)
                    navController.navigate("onboarding/podcast/$argument")
                },
                navigateNext = { navController.navigate("onboarding/tts") }
            )
        }
        composable("onboarding/tts") {
            OnboardingSpeechRoute(
                navigateNext = {
                    navController.navigate(destinationRoute) {
                        popUpTo("onboarding/folders") { inclusive = true }
                    }
                }
            )
        }
        composable(
            "onboarding/podcast/{${PodcastEditNav.FeedUriKey}}",
            arguments = listOf(navArgument(PodcastEditNav.FeedUriKey) { NavType.StringType })
        ) {
            OnboardingAddPodcastRoute(
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}