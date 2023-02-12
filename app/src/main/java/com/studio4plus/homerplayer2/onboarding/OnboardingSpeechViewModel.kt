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

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.speech.Speaker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class OnboardingSpeechViewModel(
    appContext: Context,
    private val speaker: Speaker
) : ViewModel(), DefaultLifecycleObserver {

    data class ViewState(
        val showTtsSettings: Boolean,
        val ttsEnabled: Boolean,
        val canProceed: Boolean,
        val isSpeaking: Boolean
    )

    val errorEvent = Channel<Int?>()

    private val currentState: MutableStateFlow<ViewState>
    val viewState: StateFlow<ViewState> get() = currentState

    private val ttsSettingsIntent: Intent?
    private var enableTts: Boolean
    private var ttsTestSuccessful: Boolean = false // TODO: this should be saved in prefs

    init {
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        val resolveInfo = appContext.packageManager.resolveActivity(intent, 0)
        ttsSettingsIntent = intent.takeIf { resolveInfo != null }
        enableTts = true // TODO: check accessibility service, if TalkBack enabled, disable in-app TTS.
        currentState = MutableStateFlow(ViewState(ttsSettingsIntent != null, enableTts, false, false))
    }

    fun onTtsCheckStarted() {
        errorEvent.trySend(null)
        currentState.value = currentState.value.copy(isSpeaking = true)
    }

    fun onTtsCheckFailed() {
        errorEvent.trySend(R.string.speech_init_tts_error)
    }

    fun onTtsToggled() {
        enableTts = !enableTts
        currentState.value = currentState.value.copy(
            ttsEnabled = enableTts,
            canProceed = ttsTestSuccessful || !enableTts
        )
    }

    fun say(text: CharSequence) {
        currentState.value = currentState.value.copy(isSpeaking = true)
        viewModelScope.launch {
            val initResult = withTimeoutOrNull(2000) { speaker.initIfNeeded() }
            val errorMessage = when(initResult) {
                Speaker.TtsInitResult.READY -> {
                    val success = speaker.speakAndWait(text)
                    if (success) null else R.string.speech_init_say_failed
                }
                Speaker.TtsInitResult.INIT_ERROR -> R.string.speech_init_tts_error
                Speaker.TtsInitResult.LANGUAGE_DATA_MISSING -> R.string.speech_init_lang_data_missing
                Speaker.TtsInitResult.LANGUAGE_NOT_SUPPORTED -> R.string.speech_init_lang_not_supported
                Speaker.TtsInitResult.INIT_CANCELLED -> null
                null -> R.string.speech_init_tts_error
            }
            ttsTestSuccessful = errorMessage == null
            if (errorMessage != null) errorEvent.trySend(errorMessage)
            currentState.value = currentState.value.copy(
                isSpeaking = false,
                canProceed = ttsTestSuccessful || !enableTts
            )
        }
    }

    fun openTtsSettings(activityContext: Context) {
        errorEvent.trySend(null)
        activityContext.startActivity(ttsSettingsIntent)
    }

    override fun onStop(ignored: LifecycleOwner) {
        speaker.shutdown()
    }

    override fun onCleared() {
        super.onCleared()
        speaker.shutdown()
    }
}