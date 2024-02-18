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

package com.studio4plus.homerplayer2.base.ui

import android.content.Context
import android.os.Build
import android.os.Vibrator
import org.koin.core.annotation.Factory
import java.util.Optional

// Koin doesn't support injection of nullable types and the Vibrator service is unavailable in some
// Android versions
@Factory
class VibratorProvider(appContext: Context) {
    private val vibratorService: Vibrator? = if (Build.VERSION.SDK_INT >= 26) {
        appContext.getSystemService(Vibrator::class.java)
    } else {
        null
    }

    val isAvailable: Boolean get() = vibratorService?.hasVibrator() ?: false

    val get: Vibrator? = if (isAvailable) vibratorService else null
}