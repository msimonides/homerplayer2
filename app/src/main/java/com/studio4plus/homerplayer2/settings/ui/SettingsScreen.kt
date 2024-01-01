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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studio4plus.homerplayer2.core.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.core.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.utils.composable

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val navController = rememberNavController()
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val title = currentBackStackEntry.value?.destination?.label ?: ""
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = title.toString(),
                onBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        SettingsNavHost(modifier = Modifier.padding(paddingValues), navController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
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
    NavHost(modifier = modifier, navController = navController, startDestination = "main") {
        composable("main", label = "Settings" /* TODO: resource */) {
            SettingsMain()
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
