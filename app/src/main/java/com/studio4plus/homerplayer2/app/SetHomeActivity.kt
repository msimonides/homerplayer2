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

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import com.studio4plus.homerplayer2.settings.DATASTORE_UI_SETTINGS
import com.studio4plus.homerplayer2.settings.UiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

private const val HOME_ACTIVITY_ALIAS = "com.studio4plus.homerplayer2.app.HomeActivity"

@Single(createdAtStart = true)
class SetHomeActivity(
    mainScope: CoroutineScope,
    appContext: Context,
    @Named(DATASTORE_UI_SETTINGS) uiSettings: DataStore<UiSettings>
) {

    private val fullKioskEnabled = uiSettings.data.map {
        it.fullKioskMode
    }

    init {
        fullKioskEnabled
            .onEach { isEnabled ->
                val homeComponentName = ComponentName(appContext, HOME_ACTIVITY_ALIAS)
                val enabledState =
                    if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                appContext.packageManager.setComponentEnabledSetting(
                    homeComponentName,
                    enabledState,
                    PackageManager.DONT_KILL_APP
                )
            }.launchIn(mainScope)
    }
}