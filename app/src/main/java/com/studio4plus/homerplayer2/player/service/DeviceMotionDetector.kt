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

package com.studio4plus.homerplayer2.player.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

class DeviceMotionDetector(
    private val appContext: Context
) {
    enum class MotionType {
        FACE_DOWN,
        SIGNIFICANT_MOTION,
        OTHER
    }

    val motionType = sensorFlow { MotionEventDetector { trySend(it) } }

    private class MotionEventDetector(val onUpdate: (MotionType) -> Unit) : SensorEventListener {

        private val MIN_EVENT_QUEUE_SIZE = 4
        private val MAX_EVENT_QUEUE_TIME_NS = TimeUnit.MILLISECONDS.toNanos(500)

        private var previousTimestamp: Long = 0
        private val previousValues = FloatArray(3)
        private val events = EventQueue()

        override fun onSensorChanged(event: SensorEvent) {
            if (previousTimestamp > 0) {
                var deltaSq = 0f
                for (i in 0 until 3) {
                    val delta = event.values[i] - previousValues[i]
                    deltaSq += delta*delta
                }
                val z = event.values[2]
                val g = 9.81f
                val isFaceDown = z > -g - 1f && z < -g + 1f
                val isStill = deltaSq < 1f
                val isFaceDownAndStill = isStill && isFaceDown

                if (events.size >= MIN_EVENT_QUEUE_SIZE)
                    events.removeOld(event.timestamp - MAX_EVENT_QUEUE_TIME_NS)
                events.add(event.timestamp, isMoving = !isStill, isFaceDown = isFaceDownAndStill)

                if (events.size >= MIN_EVENT_QUEUE_SIZE && events.oldestTimestampNs!! - MAX_EVENT_QUEUE_TIME_NS < event.timestamp) {
                    val motionType = when {
                        events.inMotionCount >= 0.75 * events.size -> MotionType.SIGNIFICANT_MOTION
                        events.faceDownCount >= 0.75 * events.size -> MotionType.FACE_DOWN
                        else -> MotionType.OTHER
                    }
                    onUpdate(motionType)
                }
            }
            previousTimestamp = event.timestamp

            event.values.forEachIndexed { index, value -> previousValues[index] = value }
        }

        override fun onAccuracyChanged(snesor: Sensor?, accuracy: Int) = Unit
    }

    private class EventQueue {

        data class EventEntry(
            val timestampNs: Long,
            val isMoving: Boolean,
            val isFaceDown: Boolean
        )

        private val events = ArrayList<EventEntry>()

        var faceDownCount: Int = 0
            private set
        var inMotionCount: Int = 0
            private set
        val size: Int get() = events.size
        val oldestTimestampNs: Long? get() = events.lastOrNull()?.timestampNs

        fun add(timestampNs: Long, isMoving: Boolean, isFaceDown: Boolean) {
            if (isFaceDown) ++faceDownCount
            if (isMoving) ++inMotionCount
            events.add(EventEntry(timestampNs, isMoving = isMoving, isFaceDown = isFaceDown))
        }

        fun removeOld(timestampNs: Long) {
            var oldest = events.firstOrNull()
            while (oldest != null && oldest.timestampNs < timestampNs) {
                val removed = events.removeFirst()
                onRemove(removed)
                oldest = events.firstOrNull()
            }
        }

        private fun onRemove(event: EventEntry) {
            if (event.isFaceDown) --faceDownCount
            if (event.isMoving) --inMotionCount
        }
    }

    private fun <T> sensorFlow(
        eventListenerProvider: ProducerScope<T>.() -> SensorEventListener
    ): Flow<T> = callbackFlow {
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            val listener = eventListenerProvider()
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            awaitClose { sensorManager.unregisterListener(listener) }
        } else {
            // TODO: log lack of sensor.
            awaitClose()
        }
    }
}