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
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class DeviceAdminStatus(
    private val dpm: DevicePolicyManager,
    private val appContext: Context
) {
    private val isDeviceOwnerStateFlow = MutableStateFlow(false)
    val isDeviceOwner: StateFlow<Boolean> = isDeviceOwnerStateFlow

    init {
        setIsDeviceOwner(dpm.isDeviceOwnerApp(appContext.packageName))
    }

    fun setIsDeviceOwner(isOwner: Boolean) {
        isDeviceOwnerStateFlow.value = isOwner
    }

    fun dropDeviceOwner() {
        dpm.clearDeviceOwnerApp(appContext.packageName)
        dpm.removeActiveAdmin(DeviceAdminReceiver.component(appContext))
    }
}