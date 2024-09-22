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

package com.studio4plus.homerplayer2.samplebooks

import com.studio4plus.homerplayer2.base.LocaleProvider
import io.sentry.Sentry
import org.json.JSONObject
import org.koin.core.annotation.Factory
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.io.InputStream
import java.util.Locale

private const val TITLE_FILE_NAME = "titles.json"
private const val FALLBACK_LANG = "default"

@Factory
class SamplesUnpacker(
    private val getLocale: LocaleProvider,
) {
    @Throws(IOException::class, IllegalArgumentException::class)
    operator fun invoke(zipFile: File, destinationFolder: File) =
        invoke(zipFile.inputStream(), destinationFolder)

    @Throws(IOException::class, IllegalArgumentException::class)
    operator fun invoke(inputStream: InputStream, destinationFolder: File) {
        unzip(inputStream, destinationFolder)
        localizeTitles(destinationFolder)
    }

    @Throws(IOException::class)
    private fun localizeTitles(folder: File) {
        folder.listFiles(FileFilter { it.isDirectory })?.forEach {
            try {
                val title = getTitle(it)
                it.renameTo(File(folder, title))
            } catch (e: IOException) {
                Sentry.captureException(e)
                Timber.e(e, "Error localizing files")
            }
        }
    }

    @Throws(IOException::class)
    private fun getTitle(folder: File): String {
        val locale = getLocale()
        val fullLang = with(locale) { "${language}_$country" }
        val lang = locale.language
        val jsonString = File(folder, TITLE_FILE_NAME).readText()
        return with(JSONObject(jsonString)) {
            getStringOrNull(fullLang)
                ?: getStringOrNull(lang)
                ?: getStringOrNull(FALLBACK_LANG)
                ?: folder.name
        }
    }

    private fun JSONObject.getStringOrNull(key: String) = optString(key).ifBlank { null }
}