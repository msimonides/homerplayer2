import com.android.build.gradle.api.ApplicationVariant
import com.studio4plus.homerplayer2.GenerateProvisioningDataTask

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

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sentry)
}

android {
    namespace = "com.studio4plus.homerplayer2.kiosk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.studio4plus.homerplayer2.kiosk"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        compileOptions {
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.configureEach {
        kotlin.sourceSets {
            getByName(name) { kotlin.srcDir("build/generated/ksp/${name}/kotlin") }
        }

        registerProvisioningInfoTask()
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarjdk)

    ksp(libs.koin.compiler)

    implementation(project(":base"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.sentry.android)

    testImplementation(libs.junit)
}

fun ApplicationVariant.registerProvisioningInfoTask() {
    val variantName = name
    val capitalizedName =
        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    if (outputs.size != 1) {
        throw IllegalArgumentException("Only single APK output is supported")
    }
    val output = outputs.first()

    tasks.register<GenerateProvisioningDataTask>("generateProvisioningData${capitalizedName}") {
        description = "create data for provisioning QR code"
        group = "Build"
        dependsOn += packageApplicationProvider.get()

        template.set(layout.projectDirectory.file("src/main/provisioning-qrcode.txt"))
        versionName.set(android.defaultConfig.versionName)
        apkFile.set(output.outputFile)
        outputDirectory.set(layout.buildDirectory.dir("outputs/qrcode/$variantName/"))
    }

    tasks.named("assemble${capitalizedName}").get()
        .dependsOn += "generateProvisioningData${capitalizedName}"
}
