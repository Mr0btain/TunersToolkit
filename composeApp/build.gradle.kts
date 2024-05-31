import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    kotlin("plugin.serialization") version "1.9.0" apply true
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    jvm("desktop")

    // For IOS, Not tested in any shape or form
    /*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
     */
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)

            // Context for coroutines
            implementation(libs.kotlinx.coroutines.core)

            // For ViewModel and Koin Implementation
            api(libs.precompose)
            api(libs.precompose.viewmodel)
            api(libs.precompose.koin)

            // File picker seems to not work for android
            implementation(libs.calf.file.picker)

            // Koin for Dependency injection
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Window Size class
            implementation(libs.window.size.multiplatform)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.appdirs)

            // Should really hard code this
            val lwjglVersion = "3.3.1"
            listOf("lwjgl", "lwjgl-tinyfd").forEach { lwjglDep ->
                implementation("org.lwjgl:${lwjglDep}:${lwjglVersion}")
                listOf(
                    "natives-windows",
                    "natives-windows-x86",
                    "natives-windows-arm64",
                    "natives-macos",
                    "natives-macos-arm64",
                    "natives-linux",
                    "natives-linux-arm64",
                    "natives-linux-arm32"
                ).forEach { native ->
                    runtimeOnly("org.lwjgl:${lwjglDep}:${lwjglVersion}:${native}")
                }
            }
        }
    }
}

// VERY loosely tested. crashes sometimes and requires a descent amount of ram
android {
    namespace = "org.mr0btain.tunerstoolkit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.mr0btain.tunerstoolkit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.mr0btain.tunerstoolkit"
            packageVersion = "1.0.0"
        }
    }
}
