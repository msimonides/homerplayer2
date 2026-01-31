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

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.R
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SpeechTestViewModel(appContext: Context, private val speaker: Speaker) :
    ViewModel(), DefaultLifecycleObserver {

    data class ViewState(
        val showTtsSettings: Boolean,
        val isSpeaking: Boolean,
        val ttsTestSuccessful: Boolean,
    )

    private val ttsSettingsIntent: Intent?
    private val currentState: MutableStateFlow<ViewState>
    val viewState: StateFlow<ViewState>
        get() = currentState

    val errorEvent = Channel<Int?>()

    init {
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        val resolveInfo = appContext.packageManager.resolveActivity(intent, 0)
        ttsSettingsIntent = intent.takeIf { resolveInfo != null }
        val initialState =
            ViewState(ttsSettingsIntent != null, isSpeaking = false, ttsTestSuccessful = false)
        currentState = MutableStateFlow(initialState)
    }

    fun say(text: CharSequence) {
        currentState.update { it.copy(isSpeaking = true) }
        viewModelScope.launch {
            val status = speaker.speakAndWait(text, initTimeout = Duration.INFINITE)
            val errorMessage =
                when (status) {
                    SpeakResult.SUCCESS -> null
                    SpeakResult.ERROR_TTS_INIT,
                    SpeakResult.ERROR_TTS_INIT_TIMEOUT -> R.string.speech_init_tts_error

                    SpeakResult.ERROR_LANG_MISSING_DATA -> R.string.speech_init_lang_data_missing
                    SpeakResult.ERROR_LANG_NOT_SUPPORTED -> R.string.speech_init_lang_not_supported
                    SpeakResult.ERROR_SAY -> R.string.speech_init_say_failed
                }

            if (errorMessage != null) {
                errorEvent.trySend(errorMessage)
            }
            currentState.update {
                it.copy(isSpeaking = false, ttsTestSuccessful = errorMessage == null)
            }
        }
    }

    fun openTtsSettings(activityContext: Context) {
        errorEvent.trySend(null)
        activityContext.startActivity(ttsSettingsIntent)
    }

    override fun onStop(owner: LifecycleOwner) {
        speaker.shutdown()
    }

    override fun onCleared() {
        super.onCleared()
        speaker.shutdown()
    }
}
