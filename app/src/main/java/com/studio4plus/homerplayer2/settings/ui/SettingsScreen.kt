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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.utils.composable

@Composable
fun Modifier.defaultSettingsItem() = this
    .fillMaxWidth()
    .padding(horizontal = HomerTheme.dimensions.screenContentPadding, vertical = 8.dp)
    .heightIn(min = HomerTheme.dimensions.settingsRowMinHeight)

@Composable
fun SettingsScreen(navigateBack: () -> Unit) {
    val navController = rememberNavController()
    val currentBackStackEntryState = navController.currentBackStackEntryAsState()
    val currentBackStackEntry = currentBackStackEntryState.value
    val title = currentBackStackEntry?.destination?.label?.toString()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            SettingsTopBar(
                toolbarTitle = title,
                onBack = {
                    val navigated = navController.popBackStack()
                    if (!navigated) navigateBack()
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        SettingsNavHost(
            snackbarHostState = snackbarHostState,
            closeSettings = navigateBack,
            modifier = Modifier.padding(paddingValues),
            navController = navController,
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.generic_navigate_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
    )
}

@Composable
private fun SettingsNavHost(
    closeSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val mainTitle = stringResource(id = R.string.settings_ui_settings_title)
    val aboutTitle = stringResource(id = R.string.settings_ui_about_title)
    val foldersTitle = stringResource(id = R.string.settings_ui_audiobooks_folders_title)
    val kioskSetupTitle = stringResource(id = R.string.settings_ui_kiosk_mode_setup_title)
    val lockdownTitle = stringResource(id = R.string.settings_ui_lockdown_settings_title)
    val licensesTitle = stringResource(id = R.string.settings_ui_licenses_title)
    val playbackTitle = stringResource(id = R.string.settings_ui_playback_settings_title)
    val playerUiTitle = stringResource(id = R.string.settings_ui_player_ui_title)
    val ttsTitle = stringResource(id = R.string.settings_ui_tts_settings_title)
    val uiTitle = stringResource(id = R.string.settings_ui_ui_settings_title)
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
            SettingsMainRoute(
                navigateFolders = { navController.navigate("folders") },
                navigatePlaybackSettings = { navController.navigate("playback_settings") },
                navigatePlayerUiSettings = { navController.navigate("player_ui_settings") },
                navigateLockdownSettings = { navController.navigate("lockdown_settings") },
                navigateTtsSettings = { navController.navigate("tts_settings") },
                navigateUiSettings = { navController.navigate("ui_settings") },
                navigateAbout = { navController.navigate("about") }
            )
        }
        composable("about", label = aboutTitle) {
            SettingsAboutRoute(
                navigateLicenses = { navController.navigate("licenses") },
            )
        }
        composable("folders", label = foldersTitle) {
            SettingsFoldersRoute()
        }
        composable("kiosk_setup", label = kioskSetupTitle) {
            SettingsKioskModeSetupRoute()
        }
        composable("licenses", label = licensesTitle) {
            SettingsLicenses()
        }
        composable("lockdown_settings", label = lockdownTitle) {
            SettingsLockdownRoute(
                navigateKioskModeSettings = { navController.navigate("kiosk_setup") },
                closeSettings = closeSettings
            )
        }
        composable("playback_settings", label = playbackTitle) {
            SettingsPlaybackRoute()
        }
        composable("player_ui_settings", label = playerUiTitle) {
            SettingsPlayerUiRoute()
        }
        composable("tts_settings", label = ttsTitle) {
            SettingsTtsRoute(snackbarHostState)
        }
        composable("ui_settings", label = uiTitle) {
            SettingsUiRoute()
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
