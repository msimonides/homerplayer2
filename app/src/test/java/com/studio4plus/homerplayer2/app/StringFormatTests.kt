/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
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
import androidx.test.core.app.ApplicationProvider
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.base.testing.StringFormatTester
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.studio4plus.homerplayer2.R as AppR
import com.studio4plus.homerplayer2.base.R as BaseR

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StringFormatTests {

    @Test
    fun `test app translations format strings`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        StringFormatTester.test(
            app = app,
            locales = BuildConfig.SUPPORTED_TRANSLATIONS.toList(),
            strings = AppR.string::class.java,
            plurals = AppR.plurals::class.java
        )
    }

    @Test
    fun `test base translations format strings`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        StringFormatTester.test(
            app = app,
            locales = BuildConfig.SUPPORTED_TRANSLATIONS.toList(),
            strings = BaseR.string::class.java,
            plurals = null
        )
    }
}