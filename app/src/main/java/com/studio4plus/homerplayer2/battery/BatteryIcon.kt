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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val LevelIcons = arrayOf(
    Icons.Default.Battery0Bar,
    Icons.Default.Battery1Bar,
    Icons.Default.Battery2Bar,
    Icons.Default.Battery3Bar,
    Icons.Default.Battery4Bar,
    Icons.Default.Battery5Bar,
    Icons.Default.Battery6Bar,
    Icons.Default.BatteryFull
)

private val LowBatteryThreshold = 1f / LevelIcons.size * 2
private val CriticalBatteryThreshold = 1f / LevelIcons.size

@Composable
fun BatteryIcon(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    val percentage = (100 * batteryState.level).roundToInt()
    val batteryLevelString = when (batteryState) {
        is BatteryState.Charging ->
            pluralStringResource(R.plurals.battery_level_charging_content_description, count = percentage, percentage)
        is BatteryState.Discharging ->
            pluralStringResource(R.plurals.battery_level_content_description, count = percentage, percentage)
    }
    val semantics = Modifier.semantics {
        contentDescription = batteryLevelString
    }
    when (batteryState) {
        is BatteryState.Charging -> ChargingBatteryIcon(batteryState.level, modifier.then(semantics))
        is BatteryState.Discharging -> DischargingBatteryIcon(batteryState.level, modifier.then(semantics))
    }
}

@Composable
private fun DischargingBatteryIcon(
    level: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        level < LowBatteryThreshold -> HomerTheme.colors.batteryLow
        else -> HomerTheme.colors.batteryRegular
    }
    val effectiveModifier = when {
        level < CriticalBatteryThreshold -> modifier.blink(1000)
        else -> modifier
    }
    Icon(
        imageVector = levelToIcon(level),
        contentDescription = null,
        tint = color,
        modifier = effectiveModifier
    )
}

@Composable
private fun ChargingBatteryIcon(
    level: Float,
    modifier: Modifier = Modifier
) {
    val fullIndex = LevelIcons.size - 1
    val steps = ((1f - level) * fullIndex).roundToInt()
    var iconIndex by remember { mutableStateOf(fullIndex - steps) }
    LaunchedEffect(steps) {
        while(true) {
            delay(500)
            if (iconIndex < fullIndex ) iconIndex++
            else iconIndex = fullIndex - steps
        }
    }
    Icon(
        imageVector = LevelIcons[iconIndex],
        contentDescription = null,
        tint = HomerTheme.colors.batteryRegular,
        modifier = modifier
    )
}

private fun levelToIcon(level: Float): ImageVector =
    LevelIcons[((LevelIcons.size - 1) * level).roundToInt()]

@Preview
@Composable
fun BatteryPreview() {
    HomerPlayer2Theme {
        Row {
            BatteryIcon(
                BatteryState.Discharging(0.7f),
                modifier = Modifier.size(32.dp)
            )
            BatteryIcon(
                BatteryState.Charging(0.78f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

fun Modifier.blink(delayMs: Long): Modifier = composed {
    var isVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(delayMs)
            isVisible = !isVisible
        }
    }
    graphicsLayer { alpha = if (isVisible) 1f else 0f }
}
