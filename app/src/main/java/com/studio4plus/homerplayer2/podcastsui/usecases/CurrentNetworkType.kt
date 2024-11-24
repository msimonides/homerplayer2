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

package com.studio4plus.homerplayer2.podcastsui.usecases

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.studio4plus.homerplayer2.settingsdata.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Factory

@Factory
class CurrentNetworkType(
    mainScope: CoroutineScope,
    private val connectivityManager: ConnectivityManager
) {

    private data class NetworkState(
        val network: Network,
        val networkType: NetworkType
    )

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        val currentNetworkInfo = MutableStateFlow<NetworkState?>(null)

        init {
            if (Build.VERSION.SDK_INT >= 23) {
                val currentNetwork = connectivityManager.activeNetwork
                if (currentNetwork != null) {
                    currentNetworkInfo.value = NetworkState(currentNetwork, activeNetworkType())
                }
            }
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            currentNetworkInfo.value = NetworkState(network, activeNetworkType())
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val isUnmetered =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val networkType = if (isUnmetered) NetworkType.Unmetered else NetworkType.Any
            currentNetworkInfo.value = NetworkState(network, networkType)
        }

        override fun onLost(network: Network) {
            if (network == currentNetworkInfo.value?.network) {
                currentNetworkInfo.value = null
            }
            super.onLost(network)
        }
    }

    val networkType: StateFlow<NetworkType?> = networkCallback.currentNetworkInfo
        .map { it?.networkType }
        .stateIn(mainScope, SharingStarted.Eagerly, activeNetworkType())

    init {
        if (Build.VERSION.SDK_INT >= 24) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }


    private fun activeNetworkType(): NetworkType = when {
        connectivityManager.isActiveNetworkMetered -> NetworkType.Any
        else -> NetworkType.Unmetered
    }
}