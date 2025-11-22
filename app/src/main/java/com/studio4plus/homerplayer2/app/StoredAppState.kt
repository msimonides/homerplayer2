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

package com.studio4plus.homerplayer2.app

import androidx.datastore.core.DataMigration
import com.studio4plus.homerplayer2.base.VersionUpdate
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Factory

@Serializable
data class StoredAppState(
    val onboardingCompleted: Boolean = false,
    val hasPresentSwipeGesture: Boolean = false,
    val firstRunTimestampMs: Long = UNSET_TIMESTAMP_MS,
) {
    companion object {
        const val UNSET_TIMESTAMP_MS = 0L
    }
}

@Factory
class StoredAppStateMigration1_2(val versionUpdate: VersionUpdate) : DataMigration<StoredAppState> {

    override suspend fun shouldMigrate(currentData: StoredAppState): Boolean =
        versionUpdate.updatingFromVersion <= 16

    override suspend fun cleanUp() = Unit

    override suspend fun migrate(currentData: StoredAppState): StoredAppState =
        currentData.copy(hasPresentSwipeGesture = true)
}