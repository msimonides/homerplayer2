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

package com.studio4plus.homerplayer2.app

import android.app.Application
import com.studio4plus.homerplayer2.testutils.FakeDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppPresentPlayButtonTests {

    @Test
    fun `shouldPresent emits true when hasPresentPlayButton is false`() = runTest {
        val presentPlayButton =
            AppPresentPlayButton(backgroundScope, FakeDataStore(StoredAppState()))

        val shouldPresent = presentPlayButton.shouldPresent.first()
        assertTrue("shouldPresent should be true initially", shouldPresent)
    }

    @Test
    fun `shouldPresent emits false after onUserPlayButton is called`() = runTest {
        val presentPlayButton =
            AppPresentPlayButton(backgroundScope, FakeDataStore(StoredAppState()))

        presentPlayButton.onUserPlayButton()

        val shouldPresent = presentPlayButton.shouldPresent.first()
        assertFalse("shouldPresent should be false after onUserPlayButton", shouldPresent)
    }

    @Test
    fun `hasPresentPlayButton is persisted across instances`() = runTest {
        val dataStore = FakeDataStore(StoredAppState())
        val presentPlayButton1 = AppPresentPlayButton(backgroundScope, dataStore)

        // Call onUserPlayButton
        presentPlayButton1.onUserPlayButton()

        // Check the datastore directly to see if the value persisted
        val state = dataStore.data.first()
        assertTrue(
            "DataStore should have hasPresentPlayButton=true after onUserPlayButton",
            state.hasPresentPlayButton,
        )

        // Create new instance with same dataStore - should reflect the change
        val presentPlayButton2 = AppPresentPlayButton(backgroundScope, dataStore)
        val shouldPresent = presentPlayButton2.shouldPresent.first()
        assertFalse("hasPresentPlayButton should persist across instances", shouldPresent)
    }
}
