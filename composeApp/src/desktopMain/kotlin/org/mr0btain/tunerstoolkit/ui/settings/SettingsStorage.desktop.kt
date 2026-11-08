package org.mr0btain.tunerstoolkit.ui.settings

import org.mr0btain.tunerstoolkit.platform.DirectoryResolverFactory
import java.io.File

/** Where settings have always been on desktop: <user data dir>/TunersToolkit/projects/settings.sts */
private fun settingsFile(): File = File(DirectoryResolverFactory().instance.getUserDataDir("TunersToolkit", false), "projects/settings.sts")

internal actual fun readSettingsText(): String? = settingsFile().takeIf { it.isFile }?.readText()

internal actual fun writeSettingsText(text: String) {
    val file = settingsFile()
    file.parentFile?.mkdirs()
    file.writeText(text)
}
