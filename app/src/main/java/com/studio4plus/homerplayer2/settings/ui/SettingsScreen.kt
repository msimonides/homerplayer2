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

package com.studio4plus.homerplayer2.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.core.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.core.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.utils.composable

@Composable
fun SettingsScreen(navigateBack: () -> Unit) {
    val navController = rememberNavController()
    val currentBackStackEntryState = navController.currentBackStackEntryAsState()
    val currentBackStackEntry = currentBackStackEntryState.value
    val title = currentBackStackEntry?.destination?.label?.toString()
    Scaffold(
        topBar = {
            SettingsTopBar(
                toolbarTitle = title,
                onBack = {
                    val navigated = navController.popBackStack()
                    if (!navigated) navigateBack()
                }
            )
        }
    ) { paddingValues ->
        SettingsNavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(toolbarTitle: String?, onBack: () -> Unit) {
    val animatedTitle: @Composable (title: String) -> Unit =  { title ->
        AnimatedContent(
            targetState = title,
            modifier = Modifier.fillMaxWidth(),
            label = "title transition",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { targetState ->
            Text(targetState)
        }
    }
    TopAppBar(
        title = {
            if (toolbarTitle != null) { animatedTitle(toolbarTitle) }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null) // TODO: contentDescription
            }
        }
    )
}

@Composable
private fun SettingsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val mainTitle = stringResource(id = R.string.settings_ui_settings_title)
    val foldersTitle = stringResource(id = R.string.settings_ui_audiobooks_folders)
    NavHost(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = HomerTheme.dimensions.screenContentPadding),
        navController = navController,
        startDestination = "main",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        composable("main", label = mainTitle) {
            SettingsMain(
                navigateFolders = { navController.navigate("folders") }
            )
        }
        composable("folders", label = foldersTitle) {
            SettingsFoldersRoute()
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    HomerPlayer2Theme {
        SettingsScreen({})
    }
    HomerTheme
}
