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
package com.studio4plus.homerplayer2

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject

abstract class GenerateProvisioningDataTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputFile abstract val template: RegularFileProperty

    @get:Input abstract val versionName: Property<String>

    @get:InputFile abstract val apkFile: RegularFileProperty

    @get:Inject abstract val filesystem: FileSystemOperations

    @TaskAction
    fun copyFromTemplate() {
        val expandValues = mapOf(
            "version" to versionName.get(),
            "apkChecksum" to computeApkChecksum(apkFile.get().asFile)
        )
        filesystem.copy {
            from(template)
            into(outputDirectory)
            expand(expandValues)
        }
    }

    private fun computeApkChecksum(file: File): String {
        val apkDigest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock { buffer, bytes ->
            apkDigest.update(buffer, 0, bytes)
        }
        val digestBytes = apkDigest.digest()
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(digestBytes)
    }
}
