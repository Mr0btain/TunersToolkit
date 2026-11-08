import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialize)
}

kotlin {
    // JDK 21 everywhere (Gradle downloads it through foojay when it isn't installed), the same as CI
    jvmToolchain(21)

    // Built as an Android library; the app itself is :androidApp
    android {
        namespace = "org.mr0btain.tunerstoolkit"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }

    // iOS only builds on a Mac (it needs Xcode), so elsewhere the targets are left out. Xcode builds the framework
    // through embedAndSignAppleFrameworkForXcode (iosApp's build phase)
    if (System.getProperty("os.name").startsWith("Mac")) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain = getByName("desktopMain")

        commonMain.dependencies {
            implementation(projects.core)

            // The UI kit shared with TicketMan
            implementation(libs.tm.ui.kit)

            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)

            implementation(libs.kotlin.coroutines)
            implementation(libs.kotlinx.serialization.json)

            // File open and save dialogs on every platform, including the browser
            implementation(libs.bundles.fileKit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.coroutines.test)
        }

        getByName("desktopTest").dependencies {
            // UiKitRulesTest's checks
            implementation(libs.tm.ui.kit.rules)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // OS specific app data folders (platform/*DirectoryResolver)
            implementation(libs.jna)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
}

compose.desktop {
    application {
        mainClass = "org.mr0btain.tunerstoolkit.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TunersToolkit"
            description = "ECU calibration editor"
            vendor = "Mr0btain"
            // Installers only take numbers, so a 1.0.0-rc1 is packaged as 1.0.0
            val appVersion = providers.gradleProperty("tunerstoolkit.version").get().substringBefore('-')
            packageVersion = appVersion
            modules("jdk.unsupported")

            macOS {
                bundleID = "org.mr0btain.tunerstoolkit"
                // macOS refuses a version starting with 0, so 0.x builds say 1.0.0 inside the app bundle
                packageVersion = if (appVersion.startsWith("0.")) "1.0.0" else appVersion
            }
            windows {
                // Never change this: it's how a newer MSI knows to replace the installed one
                upgradeUuid = "2f6b1f0e-9a51-4c7e-8a4e-5d3c0b7a2e91"
                menu = true
                shortcut = true
            }
        }

        // Where readTestDataFile finds testdata/ (the demo project, and real ECU files kept out of git)
        tasks.withType<JavaExec>().configureEach {
            systemProperty("tunerstoolkit.testdata", rootProject.file("testdata").path)
        }
    }
}
