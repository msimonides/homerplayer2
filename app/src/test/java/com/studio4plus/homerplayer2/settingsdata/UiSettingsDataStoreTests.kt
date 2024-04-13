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

package com.studio4plus.homerplayer2.settingsdata

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studio4plus.homerplayer2.testutils.TestDispatcherProvider
import com.studio4plus.homerplayer2.base.DispatcherProvider
import com.studio4plus.homerplayer2.loccalstorage.LocalStorageModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UiSettingsDataStoreTests {

    private lateinit var context: Context
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        // TODO: consider using koin
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        dispatcherProvider = TestDispatcherProvider(testDispatcher)
    }

    @Test
    fun `fullKioskModeEnableTimestamp updates are stored`() = testScope.runTest {
        val dataStore1Job = Job()
        val dataStore1 = SettingsDataModule().uiSettingsDatastore(
            context,
            this + dataStore1Job,
            dispatcherProvider,
            LocalStorageModule().json()
        )
        val defaultData = dataStore1.data.first()
        assertEquals(UiSettings(), defaultData)

        dataStore1.updateData {
            it.copy(fullKioskModeEnableTimestamp = FullKioskModeSetting.ENABLED)
        }
        dataStore1Job.cancel()

        val dataStore2 = SettingsDataModule().uiSettingsDatastore(
            context,
            this,
            dispatcherProvider,
            LocalStorageModule().json()
        )
        val loadedData = dataStore2.data.first()
        assertEquals(FullKioskModeSetting.ENABLED, loadedData.fullKioskModeEnableTimestamp)
    }

    @Test
    fun `fullKioskModeEnableTimestamp migration`() = testScope.runTest {
        val settingsJson = """
            { "fullKioskMode": true }
        """.trimIndent()
        val datastoreFolder = File(context.filesDir, "datastore")
        val datastoreFile = File(datastoreFolder, "uiSettings")
        assertTrue(datastoreFolder.mkdirs())
        datastoreFile.writeText(settingsJson)

        val dataStore = SettingsDataModule().uiSettingsDatastore(
            context,
            this,
            dispatcherProvider,
            LocalStorageModule().json()
        )
        val loadedData = dataStore.data.first()
        assertEquals(FullKioskModeSetting.ENABLED, loadedData.fullKioskModeEnableTimestamp)
    }
}