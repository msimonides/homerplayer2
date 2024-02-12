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

package com.studio4plus.homerplayer2.kiosk.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio4plus.homerplayer2.kiosk.Constants
import com.studio4plus.homerplayer2.kiosk.R
import com.studio4plus.homerplayer2.kiosk.deviceadmin.DeviceAdminStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import org.koin.android.annotation.KoinViewModel

data class MainScreenViewState(
    @StringRes val statusTitle: Int,
    @StringRes val statusDescription: Int,
    @StringRes val mainActionLabel: Int,
    val mainActionIntent: Intent,
    val mainActionWebsiteUrl: String?,
    val dropPrivilegeEnabled: Boolean,
)

@KoinViewModel
class MainScreenViewModel(
    private val deviceAdminStatus: DeviceAdminStatus,
    private val intents: Intents
): ViewModel() {

    private val homerPlayerLaunchIntent = MutableStateFlow(intents.openPlayer())

    val viewState: Flow<MainScreenViewState> = combine(
        deviceAdminStatus.isDeviceOwner,
        homerPlayerLaunchIntent,
    ) { isDeviceOwner, playerIntent ->
        val mainActionLabel = when {
            isDeviceOwner && playerIntent != null -> R.string.button_open_homerplayer
            isDeviceOwner -> R.string.button_install_homerplayer
            else -> R.string.button_setup_instructions_website
        }

        MainScreenViewState(
            statusTitle =
                if (isDeviceOwner) R.string.kiosk_mode_available_title
                else R.string.kiosk_mode_unavailable_title,
            statusDescription =
                if (isDeviceOwner) R.string.kiosk_mode_available_description
                else R.string.kiosk_mode_unavailable_description,
            mainActionLabel = mainActionLabel,
            mainActionIntent =
                if (isDeviceOwner) playerIntent ?: intents.installPlayer()
                else intents.openInstructions(),
            mainActionWebsiteUrl = Constants.UrlSetupInstructions.takeIf { !isDeviceOwner },
            dropPrivilegeEnabled = isDeviceOwner
        )
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    fun dropDeviceOwnerPrivilege() = deviceAdminStatus.dropDeviceOwner()
}