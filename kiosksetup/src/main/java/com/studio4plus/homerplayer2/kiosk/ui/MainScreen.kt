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

package com.studio4plus.homerplayer2.kiosk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreenRoute(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val isDeviceOwner by viewModel.isDeviceOwner.collectAsStateWithLifecycle()
    MainScreen(
        isDeviceOwner = isDeviceOwner,
        dropDeviceOwnerPrivilege = viewModel::dropDeviceOwnerPrivilege,
    )
}

@Composable
fun MainScreen(
    isDeviceOwner: Boolean,
    dropDeviceOwnerPrivilege: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(HomerTheme.dimensions.screenContentPadding)
    ) {
        val isDeviceOwnerString = if (isDeviceOwner) "yes" else "no"
        Text("Is device owner: $isDeviceOwnerString")
        // TODO: inform the user it's better to do a factory reset.
        // TODO: ask user for confirmation when dropping device owner.
        Button(
            onClick = dropDeviceOwnerPrivilege,
            enabled = isDeviceOwner
        ) {

            Text("Drop device owner privilege")
        }
    }
}