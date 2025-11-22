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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import com.studio4plus.homerplayer2.analytics.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class OnboardingSpeechViewModel(
    private val onboardingDelegate: OnboardingDelegate,
    private val analytics: Analytics,
) : ViewModel(), DefaultLifecycleObserver {

    data class ViewState(
        val readBookTitlesEnabled: Boolean,
    )

    private val currentState: MutableStateFlow<ViewState>
    val viewState: StateFlow<ViewState> get() = currentState

    init {
        currentState = MutableStateFlow(ViewState(readBookTitlesEnabled = true))
    }

    fun onTtsToggled() {
        currentState.update { it.copy(readBookTitlesEnabled = !it.readBookTitlesEnabled) }
    }

    fun confirmTtsChoice() {
        val isTtsEnabled = currentState.value.readBookTitlesEnabled
        analytics.event(
            "Onboarding.Tts",
            params = mapOf("enabled" to isTtsEnabled.toString()),
        )
        onboardingDelegate.onReadBookTitlesSet(isTtsEnabled)
    }

    fun onFinished() {
        analytics.event("Onboarding.Finished")
        onboardingDelegate.onOnboardingFinished()
    }
}