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

package com.studio4plus.homerplayer2.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.studio4plus.homerplayer2.utils.broadcastReceiver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.annotation.Factory

sealed interface BatteryState {
    val level: Float

    data class Charging(override val level: Float) : BatteryState
    data class Discharging(override val level: Float) : BatteryState
}

@Factory
class BatteryStateProvider(
    appContext: Context
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val batteryState = callbackFlow {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = broadcastReceiver { channel.trySend(getBatteryState(it)) }
        val initialState =
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        if (initialState != null) {
            channel.send(getBatteryState(initialState))
        }
        invokeOnClose { appContext.unregisterReceiver(receiver) }
    }

    private fun getBatteryState(intent: Intent): BatteryState {
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val rawLevel: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val level = rawLevel.toFloat() / scale

        return if (isCharging) BatteryState.Charging(level)
        else BatteryState.Discharging(level)
    }
}