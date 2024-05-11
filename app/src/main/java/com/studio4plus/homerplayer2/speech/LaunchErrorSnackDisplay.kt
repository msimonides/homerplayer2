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

package com.studio4plus.homerplayer2.speech

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun LaunchErrorSnackDisplay(
    errorStringEvent: Channel<Int?>,
    snackbarHostState: SnackbarHostState,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    LaunchErrorSnackDisplay(
        errorStringEvent.receiveAsFlow(),
        snackbarHostState,
        lifecycleOwner
    ) { context.getString(it) }
}

@Composable
fun <T> LaunchErrorSnackDisplay(
    errorEvent: Flow<T?>,
    snackbarHostState: SnackbarHostState,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    messageProducer: (T) -> String,
) {
    LaunchedEffect(errorEvent) {
        errorEvent
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { error ->
                if (error != null) {
                    launch {
                        snackbarHostState.showSnackbar(
                            messageProducer(error),
                            duration = SnackbarDuration.Long
                        )
                    }
                } else {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
    }
}
