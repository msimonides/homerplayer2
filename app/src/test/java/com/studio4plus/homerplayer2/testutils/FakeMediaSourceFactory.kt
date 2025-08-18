/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.testutils

import android.net.Uri
import androidx.media3.common.AdPlaybackState
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.FakeTimeline.TimelineWindowDefinition
import kotlin.collections.get

class FakeMediaSourceFactory(private val durationsMs : Map<Uri, Long>) : MediaSource.Factory {
    private val DEFAULT_UID = Unit

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        throw UnsupportedOperationException()
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        throw UnsupportedOperationException()
    }

    override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri
        val durationMs = durationsMs[uri]
        checkNotNull(durationMs)
        val timelineWindowDefinition = TimelineWindowDefinition(
            /* periodCount = */ 1,
            /* id = */ DEFAULT_UID,
            /* isSeekable = */ true,
            /* isDynamic = */ false,
            /* isLive = */ false,
            /* isPlaceholder = */ false,
            /* durationUs = */ durationMs * C.MICROS_PER_SECOND,
            /* defaultPositionUs = */ 0L,
            /* windowOffsetInFirstPeriodUs = */ 0L,
            /* adPlaybackStates = */ mutableListOf(AdPlaybackState.NONE),
            /* mediaItem = */ mediaItem
        )
        return FakeMediaSource(FakeTimeline(timelineWindowDefinition))
    }

}