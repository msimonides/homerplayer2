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

import android.content.Context
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import timber.log.Timber
import java.io.File

/**
 * Manages the filesystem layout for DAISY Online downloads.
 *
 * Layout:
 *   filesDir/daisyonline/tmp/<serviceId>/<audiobookId>/   — in-progress downloads
 *   filesDir/daisyonline/final/<serviceId>/<audiobookId>/ — complete books
 */
@Single
class DaisyOnlineFileStorage(
    private val appContext: Context,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val root: File get() = File(appContext.filesDir, "daisyonline")
    private val tmpRoot: File get() = File(root, "tmp")
    private val finalRoot: File get() = File(root, "final")

    fun getTmpBookDir(serviceId: String, audiobookId: String): File =
        File(tmpRoot, "$serviceId/$audiobookId")

    fun getFinalBookDir(serviceId: String, audiobookId: String): File =
        File(finalRoot, "$serviceId/$audiobookId")

    fun getBookFile(serviceId: String, audiobookId: String, fileId: String, inProgress: Boolean): File {
        val dir = if (inProgress) getTmpBookDir(serviceId, audiobookId) else getFinalBookDir(serviceId, audiobookId)
        return File(dir, fileId)
    }

    suspend fun ensureTmpBookDir(serviceId: String, audiobookId: String): File =
        withContext(dispatcherProvider.Io) {
            getTmpBookDir(serviceId, audiobookId).also { it.mkdirs() }
        }

    /**
     * Atomically moves all files from the tmp dir to the final dir for a book.
     * Creates the final dir if needed. Deletes the tmp dir afterwards.
     */
    suspend fun moveTmpToFinal(serviceId: String, audiobookId: String) {
        withContext(dispatcherProvider.Io) {
            val tmpDir = getTmpBookDir(serviceId, audiobookId)
            val finalDir = getFinalBookDir(serviceId, audiobookId)
            finalDir.mkdirs()
            tmpDir.listFiles()?.forEach { file ->
                val dest = File(finalDir, file.name)
                if (!file.renameTo(dest)) {
                    // renameTo can fail across filesystems; fall back to copy + delete
                    file.copyTo(dest, overwrite = true)
                    file.delete()
                }
            }
            tmpDir.deleteRecursively()
            Timber.i("DaisyOnline: moved $audiobookId from tmp to final")
        }
    }

    /**
     * Deletes all files in the tmp dir for a single book.
     */
    suspend fun deleteTmpBookFiles(serviceId: String, audiobookId: String) {
        withContext(dispatcherProvider.Io) {
            getTmpBookDir(serviceId, audiobookId).deleteRecursively()
        }
    }

    /**
     * Deletes all files (tmp and final) for a single book.
     */
    suspend fun deleteBookFiles(serviceId: String, audiobookId: String) {
        withContext(dispatcherProvider.Io) {
            getTmpBookDir(serviceId, audiobookId).deleteRecursively()
            getFinalBookDir(serviceId, audiobookId).deleteRecursively()
        }
    }

    /**
     * Cleans up tmp dirs for books that are NOT in [activeAudiobookIds] for the given service.
     * Call this after a sync or download pass to remove orphaned tmp data.
     */
    suspend fun cleanupStaleTmpDirs(serviceId: String, activeAudiobookIds: Set<String>) {
        withContext(dispatcherProvider.Io) {
            val serviceTmpDir = File(tmpRoot, serviceId)
            serviceTmpDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name !in activeAudiobookIds) {
                    Timber.i("DaisyOnline: deleting stale tmp dir ${dir.absolutePath}")
                    dir.deleteRecursively()
                }
            }
        }
    }
}
