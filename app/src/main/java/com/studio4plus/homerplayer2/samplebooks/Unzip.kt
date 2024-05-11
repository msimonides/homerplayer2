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

import io.sentry.Sentry
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Throws(IOException::class, IllegalArgumentException::class)
fun unzip(zipStream: InputStream, destinationFolder: File) {
    try {
        ZipInputStream(zipStream).use { zip ->
            val canonicalDstPath = destinationFolder.canonicalFile.toString()
            var entry: ZipEntry? = zip.nextEntry
            require(entry != null) { "Zero ZIP entries in file" }
            while (entry != null) {
                val file = File(canonicalDstPath, entry.name).canonicalFile
                require(file.toString().startsWith(canonicalDstPath)) {
                    // This should never happen with the samples ZIP file.
                    "ZIP entry points outside target directory: " + entry?.name
                }

                if (entry.isDirectory) {
                    if (!file.mkdirs()) throw IOException("Unable to create directory: " + file.absolutePath)
                } else {
                    val folder: File? = file.parentFile
                    if (folder?.exists() == false) {
                        if (!folder.mkdirs()) throw IOException("Unable to create directory: " + file.absolutePath)
                    }
                    copyData(zip, file)
                }
                entry = zip.nextEntry
            }
        }
    } catch (e: IllegalArgumentException) {
        Sentry.captureException(e)
        Timber.e(e, "Invalid file")
        throw e
    } catch (e: IOException) {
        Timber.e(e, "Error unzipping file")
        throw e
    }
}

@Throws(IOException::class)
private fun copyData(zip: ZipInputStream, destinationFile: File) {
    destinationFile.sink().buffer().use {
        it.writeAll(zip.source())
    }
}