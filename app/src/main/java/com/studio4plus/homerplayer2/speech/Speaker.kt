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

package com.studio4plus.homerplayer2.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.studio4plus.homerplayer2.base.LocaleProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Speaker {
    enum class TtsInitResult {
        READY,
        LANGUAGE_DATA_MISSING,
        LANGUAGE_NOT_SUPPORTED,
        INIT_ERROR,
        INIT_CANCELLED
    }

    suspend fun initIfNeeded(): TtsInitResult
    suspend fun speakAndWait(text: CharSequence): Boolean
    fun stop()
    fun shutdown()
}

@Factory
class SpeakerTts(
    private val appContext: Context,
    private val getLocale: LocaleProvider
) : Speaker {

    private var speech: TextToSpeech? = null
    private var ttsInitialized: Deferred<Boolean>? = null

    private val utteranceListener = UtteranceListener()
    private val utteranceEvents: Flow<UtteranceListener.Event> get() = utteranceListener.events

    override suspend fun initIfNeeded(): Speaker.TtsInitResult {
        val initSuccessful: Boolean = ttsInitialized?.await()
            ?: coroutineScope {
                async { initTts() }
            }.run {
                ttsInitialized = this
                await().also {
                    ttsInitialized = null
                }
            }

        return if (initSuccessful) {
            when(speech?.isLanguageAvailable(getLocale())) {
                TextToSpeech.LANG_NOT_SUPPORTED -> {
                    Timber.w("TTS language not supported.")
                    Speaker.TtsInitResult.LANGUAGE_NOT_SUPPORTED
                }
                TextToSpeech.LANG_MISSING_DATA -> {
                    Timber.w("TTS language data missing.")
                    Speaker.TtsInitResult.LANGUAGE_DATA_MISSING
                }
                null -> Speaker.TtsInitResult.INIT_CANCELLED
                else -> Speaker.TtsInitResult.READY
            }
        } else {
            Speaker.TtsInitResult.INIT_ERROR
        }
    }

    override suspend fun speakAndWait(text: CharSequence): Boolean {
        if (speech != null) {
            val id = UUID.randomUUID().toString()
            speech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            val event = utteranceEvents.first { it.utteranceId == id }
            if (event is UtteranceListener.Event.Error) {
                Timber.w("Utterance error: %d.", event.errorCode)
            }
            return event is UtteranceListener.Event.Success
        } else {
            return false
        }
    }

    override fun stop() {
        speech?.stop()
    }

    override fun shutdown() {
        speech?.shutdown()
        speech = null
    }

    private suspend fun initTts(): Boolean = suspendCoroutine { continuation ->
        speech = TextToSpeech(appContext) {
            continuation.resume(it == TextToSpeech.SUCCESS)
        }
        speech?.setOnUtteranceProgressListener(utteranceListener)
    }

    private class UtteranceListener : UtteranceProgressListener() {

        sealed class Event(val utteranceId: String) {
            class Success(utteranceId: String) : Event(utteranceId)
            class Error(utteranceId: String, val errorCode: Int) : Event(utteranceId)
        }

        val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

        override fun onStart(utteranceId: String) = Unit

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            super.onStop(utteranceId, interrupted)
            events.tryEmit(Event.Success(utteranceId))
        }

        override fun onDone(utteranceId: String) {
            events.tryEmit(Event.Success(utteranceId))
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            events.tryEmit(Event.Error(utteranceId, errorCode))
        }

        @Deprecated("Deprecated in SDK")
        override fun onError(utteranceId: String) {
            throw UnsupportedOperationException() // The other onError should be called instead.
        }
    }
}