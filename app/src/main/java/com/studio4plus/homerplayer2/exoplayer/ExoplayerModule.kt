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

package com.studio4plus.homerplayer2.exoplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named

@Module
class ExoplayerModule {

    @Factory
    @Named(PLAYBACK)
    @OptIn(UnstableApi::class)
    fun exoplayerPlayback(appContext: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = commonBuilder(appContext)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(30_000)
            .setAudioAttributes(audioAttributes, true)
            .build()
        val audioOffloadPreferences =
            AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                .setIsGaplessSupportRequired(true)
                .setIsSpeedChangeSupportRequired(true)
                .build()
        val trackSelectionParameters =  player.trackSelectionParameters
            .buildUpon()
            .setAudioOffloadPreferences(audioOffloadPreferences)
            .build()
        return player.apply {
            this.trackSelectionParameters = trackSelectionParameters
        }
    }

    @Factory
    @Named(UTILITY)
    @OptIn(UnstableApi::class)
    fun exoplayerUtility(appContext: Context): Lazy<ExoPlayer> = lazy { commonBuilder(appContext).build() }

    @OptIn(UnstableApi::class)
    private fun commonBuilder(appContext: Context): ExoPlayer.Builder {
        // TODO: consider: ConstantBitrateSeekingEnabled
        val audioRenderersFactory = RenderersFactory { eventHandler, _, audioRendererEventListener, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    appContext,
                    MediaCodecSelector.DEFAULT,
                    eventHandler,
                    audioRendererEventListener
                )
            )
        }
        return ExoPlayer.Builder(appContext, audioRenderersFactory)
    }

    companion object {
        const val PLAYBACK = "playback"
        const val UTILITY = "utility"
    }
}