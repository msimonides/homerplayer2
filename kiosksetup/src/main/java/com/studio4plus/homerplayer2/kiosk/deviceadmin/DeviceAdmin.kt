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

package com.studio4plus.homerplayer2.kiosk.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TARGET_APP_PACKAGE = "com.studio4plus.homerplayer2"
private const val TARGET_HOME_ACTIVITY_CLASS = "com.studio4plus.homerplayer2.app.HomeActivity"

@Factory
class DeviceAdmin(
    private val appContext: Context,
    private val dpm: DevicePolicyManager,
    private val deviceAdminStatus: DeviceAdminStatus
) {
    fun onDeviceOwnerChanged() {
        val isOwner = dpm.isDeviceOwnerApp(appContext.packageName)
        if (isOwner) {
            dpm.setLockTaskPackages(
                DeviceAdminReceiver.component(appContext),
                arrayOf(TARGET_APP_PACKAGE)
            )
            setPreferredHomeActivity(
                appContext,
                dpm,
                ComponentName(TARGET_APP_PACKAGE, TARGET_HOME_ACTIVITY_CLASS)
            )
            deviceAdminStatus.setIsDeviceOwner(true)
        }
    }

    fun onBeforeDisabled() {
        deviceAdminStatus.setIsDeviceOwner(false)
        // On some buggy devices the app is no longer a device owner at this point.
        if (dpm.isDeviceOwnerApp(appContext.packageName)) {
            dpm.clearPackagePersistentPreferredActivities(
                DeviceAdminReceiver.component(appContext),
                TARGET_APP_PACKAGE
            )
        }
    }

    private fun setPreferredHomeActivity(
        context: Context,
        dpm: DevicePolicyManager,
        activityComponentName: ComponentName
    ) {
        val homeIntentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            DeviceAdminReceiver.component(context),
            homeIntentFilter,
            activityComponentName
        )
    }
}

class DeviceAdminChangedReceiver : BroadcastReceiver(), KoinComponent {

    private val deviceAdmin: DeviceAdmin by inject()
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED) {
            deviceAdmin.onDeviceOwnerChanged()
        }
    }
}

class DeviceAdminReceiver : android.app.admin.DeviceAdminReceiver(), KoinComponent {

    private val deviceAdmin: DeviceAdmin by inject()

    override fun onDisabled(context: Context, intent: Intent) {
        deviceAdmin.onBeforeDisabled()
        super.onDisabled(context, intent)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, DeviceAdminReceiver::class.java)
    }
}