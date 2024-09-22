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

package com.studio4plus.homerplayer2.base

import android.content.Context
import androidx.core.os.ConfigurationCompat
import org.koin.core.annotation.Factory
import java.util.Locale

fun interface LocaleProvider {
    operator fun invoke(): Locale
}

@Factory
class DefaultLocaleProvider(
    private val appContext: Context
) : LocaleProvider {
    override operator fun invoke(): Locale =
        // Not sure if there's a difference between locale from configuration on the app Context
        // and Locale.getDefault() - both react to device language change in testing.
        // It might be a problem for per-app language settings on Android lower than 13
        // (https://issuetracker.google.com/issues/243457462) but that's not being used.
        ConfigurationCompat.getLocales(appContext.resources.configuration).get(0)
            ?: Locale.getDefault()
}