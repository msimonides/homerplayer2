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
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Factory
import timber.log.Timber

interface Speaker {
    suspend fun warmUp()

    suspend fun speakAndWait(
        text: CharSequence,
        initTimeout: Duration = 2.seconds,
    ): SpeakResult

    fun stop()

    fun shutdown()
}

enum class SpeakResult {
    SUCCESS,
    ERROR_TTS_INIT,
    ERROR_TTS_INIT_TIMEOUT,
    ERROR_LANG_MISSING_DATA,
    ERROR_LANG_NOT_SUPPORTED,
    ERROR_SAY,
}

enum class TtsInitError {
    TtsError,
    LanguageDataMissing,
    LanguageNotSupported,
}

class TtsInitException(val error: TtsInitError, cause: Throwable? = null) : Exception(cause)

@Factory
class SpeakerTts(private val appContext: Context, private val getLocale: LocaleProvider) : Speaker {

    private var tts: TextToSpeech? = null
    private val ttsInitMutex = Mutex()

    private val utteranceListener = UtteranceListener()
    private val utteranceEvents: Flow<UtteranceListener.Event>
        get() = utteranceListener.events

    override suspend fun warmUp() {
        try {
            getTts()
        } catch (e: TtsInitException) {
            Timber.w(e, "TTS warmup failed.")
        }
    }

    override suspend fun speakAndWait(text: CharSequence, initTimeout: Duration): SpeakResult {
        try {
            val localTts =
                withTimeoutOrNull(initTimeout) { getTts() }
                    ?: return SpeakResult.ERROR_TTS_INIT_TIMEOUT

            val id = UUID.randomUUID().toString()

            val result = localTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (result == TextToSpeech.ERROR) {
                Timber.w("TTS speak failed.")
                return SpeakResult.ERROR_SAY
            }

            val event = utteranceEvents.filter { it.utteranceId == id }.first()
            if (event is UtteranceListener.Event.Error) {
                Timber.w("Utterance error: %d.", event.errorCode)
                return SpeakResult.ERROR_SAY
            }
            return SpeakResult.SUCCESS
        } catch (e: TtsInitException) {
            return e.error.toSpeakResult()
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private suspend fun getTts(): TextToSpeech {
        tts?.let {
            return it
        }
        return ttsInitMutex.withLock {
            // Another coroutine might have initialized it while we were waiting for the lock.
            tts?.let {
                return it
            }

            val newTts = suspendCoroutine { continuation ->
                var tts: TextToSpeech? = null
                tts =
                    TextToSpeech(appContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            continuation.resume(tts!!)
                        } else {
                            Timber.e("TTS init failed with status: $status")
                            continuation.resumeWithException(
                                TtsInitException(TtsInitError.TtsError)
                            )
                        }
                    }
            }
            newTts.setOnUtteranceProgressListener(utteranceListener)

            when (val langResult = newTts.isLanguageAvailable(getLocale())) {
                TextToSpeech.LANG_NOT_SUPPORTED,
                TextToSpeech.LANG_MISSING_DATA -> {
                    val error =
                        if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            TtsInitError.LanguageNotSupported
                        } else {
                            TtsInitError.LanguageDataMissing
                        }
                    Timber.w("TTS language not available: $error")
                    newTts.shutdown()
                    throw TtsInitException(error)
                }
            }

            this.tts = newTts
            newTts
        }
    }

    private fun TtsInitError.toSpeakResult(): SpeakResult =
        when (this) {
            TtsInitError.TtsError -> SpeakResult.ERROR_TTS_INIT
            TtsInitError.LanguageDataMissing -> SpeakResult.ERROR_LANG_MISSING_DATA
            TtsInitError.LanguageNotSupported -> SpeakResult.ERROR_LANG_NOT_SUPPORTED
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
            events.tryEmit(Event.Error(utteranceId, -1))
        }
    }
}
