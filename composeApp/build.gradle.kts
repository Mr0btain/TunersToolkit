import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
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
            implementation(project(":core"))

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
            // -PallPlatforms takes every desktop system's Skia, so packageUberJarForCurrentOS makes one jar that runs
            // on any of them (with Java 21 installed). Otherwise only this machine's, which the installers bundle
            if (project.hasProperty("allPlatforms")) {
                implementation(compose.desktop.linux_x64)
                implementation(compose.desktop.linux_arm64)
                implementation(compose.desktop.windows_x64)
                implementation(compose.desktop.windows_arm64)
                implementation(compose.desktop.macos_x64)
                implementation(compose.desktop.macos_arm64)
            } else {
                implementation(compose.desktop.currentOs)
            }
            implementation(libs.kotlinx.coroutinesSwing)

            // OS specific app data folders (platform/*DirectoryResolver)
            implementation(libs.jna)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
}

// Release file names say what they're for: TunersToolkit-<OS>-<version>-<arch>.<ext>, like
// TunersToolkit-Windows-0.1.0-x64.msi, for the machine that builds them, since jpackage only packages for its own system
val releaseVersion = providers.gradleProperty("tunerstoolkit.version").get()
val hostOs = System.getProperty("os.name").let {
    when {
        it.startsWith("Windows") -> "Windows"
        it.startsWith("Mac") -> "macOS"
        else -> "Linux"
    }
}
val hostArch = when (val arch = System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x64"
    "aarch64", "arm64" -> "arm64"
    else -> arch
}
fun releaseName(extension: String) = "TunersToolkit-$hostOs-$releaseVersion-$hostArch.$extension"

compose.desktop {
    application {
        mainClass = "org.mr0btain.tunerstoolkit.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TunersToolkit"
            description = "ECU calibration editor"
            vendor = "Mr0btain"
            copyright = "Copyright (c) 2026 Mr0btain. MIT licence"
            licenseFile.set(rootProject.file("LICENSE"))
            // Installers only take numbers, so a 1.0.0-rc1 is packaged as 1.0.0
            val appVersion = providers.gradleProperty("tunerstoolkit.version").get().substringBefore('-')
            packageVersion = appVersion
            modules("jdk.unsupported")

            linux {
                iconFile.set(project.file("icons/icon.png"))
                menuGroup = "Development"
            }
            macOS {
                bundleID = "org.mr0btain.tunerstoolkit"
                // The icons are made by tools/make_icons.py
                iconFile.set(project.file("icons/icon.icns"))
                // macOS refuses a version starting with 0, so 0.x builds say 1.0.0 inside the app bundle. The DMG's
                // file name still carries the real version (renamed below)
                packageVersion = if (appVersion.startsWith("0.")) "1.0.0" else appVersion
            }
            windows {
                // Never change this: it's how a newer MSI knows to replace the installed one
                upgradeUuid = "2f6b1f0e-9a51-4c7e-8a4e-5d3c0b7a2e91"
                iconFile.set(project.file("icons/icon.ico"))
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

// jpackage names the installers after packageName and packageVersion (tunerstoolkit_0.1.0_amd64.deb), so they're
// renamed once made. The macOS one gets the real version this way too, not the 1.0.0 inside its app bundle
tasks.withType<AbstractJPackageTask>().configureEach {
    val installerDir = destinationDir
    doLast {
        installerDir.get().asFile.listFiles { file -> file.extension in setOf("msi", "dmg", "deb", "pkg", "exe", "rpm") }
            ?.filter { it.name != releaseName(it.extension) }
            ?.forEach { file ->
                val named = File(file.parentFile, releaseName(file.extension))
                named.delete()
                check(file.renameTo(named)) { "Couldn't rename ${file.name} to ${named.name}" }
            }
    }
}

// With -PallPlatforms the jar runs on any desktop system, so it carries no OS or architecture. Compose's uber jar
// tasks are the plain org.gradle.jvm.tasks.Jar
tasks.withType<org.gradle.jvm.tasks.Jar>().matching { it.name.endsWith("UberJarForCurrentOS") }.configureEach {
    archiveFileName.set(
        if (project.hasProperty("allPlatforms")) "TunersToolkit-Desktop-$releaseVersion.jar" else releaseName("jar")
    )
}

// The app unpacked (bin/TunersToolkit starts it), for Linux systems without dpkg: compose/binaries/main/tar/
tasks.register<Tar>("packageTarball") {
    group = "compose desktop"
    description = "Packs the app made by createDistributable into a .tar.gz"
    from(tasks.named("createDistributable"))
    // Gradle 9's reproducible archives make every file 644, which would leave bin/TunersToolkit unable to run
    useFileSystemPermissions()
    compression = Compression.GZIP
    archiveFileName.set(releaseName("tar.gz"))
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tar"))
}

// The web app as one zip to put on any static host: build/dist/web/TunersToolkit-Web-<version>.zip
tasks.register<Zip>("packageWebZip") {
    group = "kotlin browser"
    description = "Zips the production web build"
    from(tasks.named("wasmJsBrowserDistribution"))
    archiveFileName.set("TunersToolkit-Web-$releaseVersion.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist/web"))
}
