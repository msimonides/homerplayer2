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

package com.studio4plus.homerplayer2.daisyonline

import com.studio4plus.homerplayer2.daisyonline.data.DaisyOnlineServiceEntity
import timber.log.Timber

/**
 * Wraps [DaisyOnlineSoapClient] with automatic login and single retry on session expiry.
 * One instance per service; created via Koin @Factory.
 */
class DaisyOnlineSessionManager(
    private val service: DaisyOnlineServiceEntity,
    private val soapClient: DaisyOnlineSoapClient,
) {
    /**
     * Executes a SOAP call [block], automatically logging in if needed.
     * On [DaisyOnlineSoapResult.NoActiveSession], logs in once and retries.
     * Returns [DaisyOnlineSoapResult.Error] if login fails or the retry also fails with NoActiveSession.
     */
    suspend fun <T> withSession(
        block: suspend DaisyOnlineSoapClient.(url: String) -> DaisyOnlineSoapResult<T>,
    ): DaisyOnlineSoapResult<T> {
        if (!soapClient.hasSession) {
            val loginResult = login()
            if (loginResult != null) return loginResult
        }

        val result = soapClient.block(service.url)
        return if (result is DaisyOnlineSoapResult.NoActiveSession) {
            Timber.i("DaisyOnline session expired for ${service.id}, re-logging in")
            soapClient.clearSession()
            val loginResult = login()
            if (loginResult != null) return loginResult
            val retryResult = soapClient.block(service.url)
            if (retryResult is DaisyOnlineSoapResult.NoActiveSession) {
                DaisyOnlineSoapResult.Error("No active session after re-login for service ${service.id}")
            } else {
                retryResult
            }
        } else {
            result
        }
    }

    private suspend fun login(): DaisyOnlineSoapResult.Error? {
        return when (val loginResult = soapClient.logOn(service.url, service.username, service.password)) {
            is DaisyOnlineSoapResult.Success -> {
                if (!loginResult.value) {
                    DaisyOnlineSoapResult.Error("Login failed for service ${service.id}")
                } else {
                    null
                }
            }
            is DaisyOnlineSoapResult.NoActiveSession ->
                DaisyOnlineSoapResult.Error("Unexpected NoActiveSession during logOn for service ${service.id}")
            is DaisyOnlineSoapResult.Error -> loginResult
        }
    }
}
