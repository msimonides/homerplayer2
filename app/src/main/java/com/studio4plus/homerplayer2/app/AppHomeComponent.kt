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

package com.studio4plus.homerplayer2.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.studio4plus.homerplayer2.fullkioskmode.HomeComponent
import org.koin.core.annotation.Factory

private const val HOME_ACTIVITY_ALIAS = "com.studio4plus.homerplayer2.app.HomeActivity"

@Factory
class AppHomeComponent(
    private val appContext: Context
) : HomeComponent {

    override fun setEnabled(enable: Boolean) {
        val homeComponentName = ComponentName(appContext, HOME_ACTIVITY_ALIAS)
        val enabledState =
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        appContext.packageManager.setComponentEnabledSetting(
            homeComponentName,
            enabledState,
            PackageManager.DONT_KILL_APP
        )
    }
}