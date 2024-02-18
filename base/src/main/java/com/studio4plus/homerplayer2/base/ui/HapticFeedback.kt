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

import android.annotation.TargetApi
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@TargetApi(26)
class HomerHapticFeedback(private val vibrator: Vibrator) : HapticFeedback {

    private val hasAmplitudeControl = vibrator.hasAmplitudeControl()

    // The effect is greatly affected by vibration hardware.
    // TODO: use the newer APIs for somewhat consistent effects and "harsh" values for older
    //  APIs that should work on also on low-end devices.
    private val click = VibrationEffect.createOneShot(60, amplitude(100))
    private val forward = VibrationEffect.createWaveform(
        longArrayOf(30, 150, 60),
        amplitudes(100, 0, 150),
        -1
    )
    private val back = VibrationEffect.createWaveform(
        longArrayOf(60, 150, 30),
        amplitudes(150, 0, 100),
        -1
    )

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            HomerHapticFeedbackType.Click -> vibrator.vibrate(click)
            HomerHapticFeedbackType.Back -> vibrator.vibrate(back)
            HomerHapticFeedbackType.Forward -> vibrator.vibrate(forward)
        }
    }

    private fun amplitudes(vararg amplitudes: Int): IntArray =
        IntArray(amplitudes.size).apply {
            amplitudes.forEachIndexed { index, a ->
                this[index] = amplitude(a)
            }
        }

    private fun amplitude(amplitude: Int): Int = when {
        hasAmplitudeControl -> amplitude
        amplitude == 0 -> 0
        else -> VibrationEffect.DEFAULT_AMPLITUDE
    }


}

class NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        /* Do nothing */
    }

}

object HomerHapticFeedbackType {
    val Click = HapticFeedbackType(0)
    val Forward = HapticFeedbackType(1)
    val Back = HapticFeedbackType(2)
}