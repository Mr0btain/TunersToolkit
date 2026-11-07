import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Pure logic with no Compose: file formats, the A2L model, decoding, logging and results. Everything here is tested
 * with plain kotlin.test on the JVM (:core:jvmTest) and runs on every target the app does
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.serialize)
}

// BuildInfo.VERSION (tunerstoolkit.version), so logs and bug reports carry the version they came from
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/buildInfo")
    val appVersion = providers.gradleProperty("tunerstoolkit.version").get()
    inputs.property("version", appVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("org/mr0btain/tunerstoolkit/core/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package org.mr0btain.tunerstoolkit.core\n\nobject BuildInfo {\n    const val VERSION = \"$appVersion\"\n}\n"
        )
    }
}

kotlin {
    // JDK 21 everywhere (Gradle downloads it through foojay when it isn't installed), the same as CI
    jvmToolchain(21)

    android {
        namespace = "org.mr0btain.tunerstoolkit.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // iOS only builds on a Mac (it needs Xcode), so elsewhere the targets are left out
    if (System.getProperty("os.name").startsWith("Mac")) {
        iosArm64()
        iosSimulatorArm64()
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildInfo)
        }

        commonMain.dependencies {
            implementation(libs.kotlin.coroutines)
            implementation(libs.kotlinx.serialization.json)

            // AppLogger and LogBook, shared with TicketMan
            api(libs.tm.applogger)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// DemoFilesTest rewrites testdata/demo with -DregenerateDemo=true
tasks.withType<Test>().configureEach {
    providers.systemProperty("regenerateDemo").orNull?.let { systemProperty("regenerateDemo", it) }
}
