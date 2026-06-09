import com.studio4plus.homerplayer2.GenerateProvisioningDataTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    alias(libs.plugins.ksp)
    alias(libs.plugins.sentry)
}

val kioskVersionName = "1.5.7"

android {
    namespace = "com.studio4plus.homerplayer2.kiosk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.studio4plus.homerplayer2.kiosk"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = kioskVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = if (project.hasProperty("keystorePath")) file(project.property("keystorePath") as String) else null
            storePassword = if (project.hasProperty("keystorePassword")) project.property("keystorePassword") as String else null
            keyAlias = if (project.hasProperty("keyAlias")) project.property("keyAlias") as String else null
            keyPassword = if (project.hasProperty("keyPassword")) project.property("keyPassword") as String else null
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
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

}

androidComponents {
    onVariants(selector().all()) { variant ->
        val capitalizedName =
            variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        tasks.register<GenerateProvisioningDataTask>("generateProvisioningData${capitalizedName}") {
            description = "create data for provisioning QR code"
            group = "Build"
            dependsOn("package${capitalizedName}")

            template.set(layout.projectDirectory.file("src/main/provisioning-qrcode.txt"))
            versionName.set(kioskVersionName)
            apkFile.set(
                layout.buildDirectory.file("outputs/apk/${variant.name}/${project.name}-${variant.name}.apk")
            )
            outputDirectory.set(layout.buildDirectory.dir("outputs/qrcode/${variant.name}/"))
        }

        tasks.configureEach {
            if (name == "assemble${capitalizedName}") {
                dependsOn("generateProvisioningData${capitalizedName}")
            }
        }
    }
}

sentry {
    tracingInstrumentation {
        enabled = false
        logcat {
            enabled = false
        }
    }
    autoInstallation {
        enabled = false
    }

    ignoredBuildTypes = listOf("debug")
}

dependencies {
    coreLibraryDesugaring(libs.desugarjdk)

    ksp(libs.koin.compiler)

    implementation(project(":base"))
    implementation(project(":crash:sentry"))

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

    testImplementation(libs.junit)
}

