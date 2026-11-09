import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // JDK 21 everywhere (Gradle downloads it through foojay when it isn't installed), the same as CI
    jvmToolchain(21)

    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    dependencies {
        implementation(project(":composeApp"))
        implementation(project(":core"))
        implementation(libs.androidx.activity.compose)
        implementation(libs.ui.tooling.preview)
    }
}

// 0.1.0 is versionCode 100 and 1.2.3 is 10203, so each release installs over the one before
val appVersion = providers.gradleProperty("tunerstoolkit.version").get()
val appVersionCode = appVersion.substringBefore('-').split('.').map { it.toInt() }
    .let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

// The release signing key, from env vars (CI) or ~/.gradle/gradle.properties. Without it the release APK is unsigned
fun signingSetting(env: String, property: String): String? =
    providers.environmentVariable(env).orElse(providers.gradleProperty(property)).orNull?.takeIf { it.isNotBlank() }

val releaseKeystore = signingSetting("TUNERSTOOLKIT_KEYSTORE", "tunerstoolkit.keystore")

android {
    namespace = "org.mr0btain.tunerstoolkit.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.mr0btain.tunerstoolkit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersion
    }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = signingSetting("TUNERSTOOLKIT_KEYSTORE_PASSWORD", "tunerstoolkit.keystorePassword")
                keyAlias = signingSetting("TUNERSTOOLKIT_KEY_ALIAS", "tunerstoolkit.keyAlias")
                keyPassword = signingSetting("TUNERSTOOLKIT_KEY_PASSWORD", "tunerstoolkit.keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// AGP names the APK androidApp-release(-unsigned).apk, so a copy named like the other release files goes beside it:
// TunersToolkit-Android-<version>.apk, or TunersToolkit-Android-<version>-unsigned.apk without a signing key
tasks.matching { it.name == "assembleRelease" }.configureEach {
    val apkDir = layout.buildDirectory.dir("outputs/apk/release")
    doLast {
        val dir = apkDir.get().asFile
        dir.listFiles { file -> file.name.startsWith("TunersToolkit-") }?.forEach { it.delete() }
        // The newest, in case an earlier build left a signed or unsigned one
        val apk = dir.listFiles { file -> file.extension == "apk" }?.maxByOrNull { it.lastModified() }
            ?: error("No APK in $dir")
        val suffix = if (apk.name.contains("unsigned")) "-unsigned" else ""
        apk.copyTo(File(dir, "TunersToolkit-Android-$appVersion$suffix.apk"))
    }
}
