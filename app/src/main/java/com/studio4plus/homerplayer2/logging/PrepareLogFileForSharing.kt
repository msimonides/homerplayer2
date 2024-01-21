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

package com.studio4plus.homerplayer2.logging

import android.os.Build
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.runInterruptible
import org.koin.core.annotation.Factory
import java.io.File
import java.io.PrintWriter
import java.io.Writer

@Factory
class PrepareLogFileForSharing(
    private val dispatcherProvider: DispatcherProvider,
    private val logsPathProvider: LogsPathProvider,
) {

    suspend operator fun invoke(): File =
        runInterruptible(dispatcherProvider.Io) {
            createFileForSharing(logsPathProvider.logsFolder(), logsPathProvider.sharedFolder())
        }

    private fun createFileForSharing(logsFolder: File, outputFolder: File): File {
        // Writing to private folder, createTempFile is good enough.
        outputFolder.mkdirs()
        val resultFile = createTempFile(directory = outputFolder, prefix = "homer_", suffix = ".log")
        resultFile.bufferedWriter().use { writer ->
            writer.appendDeviceInfo()
            logsFolder.listFiles()?.forEach { inputFile ->
                inputFile.bufferedReader().use { reader -> reader.copyTo(writer) }
            }
        }
        return resultFile
    }

    private fun Writer.appendDeviceInfo() {
        with(PrintWriter(this)) {
            println("Manufacturer: ${Build.MANUFACTURER}; ${Build.BRAND}")
            println("Model: ${Build.MODEL}")
            println("Android API: ${Build.VERSION.SDK_INT}")
            println("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }
    }
}