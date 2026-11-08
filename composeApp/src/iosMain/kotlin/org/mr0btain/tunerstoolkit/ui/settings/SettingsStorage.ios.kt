package org.mr0btain.tunerstoolkit.ui.settings

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** Where settings have always been on iOS: Documents/projects/settings.sts */
@OptIn(ExperimentalForeignApi::class)
private fun settingsPath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
    val folder = "${documents?.path}/projects"
    NSFileManager.defaultManager.createDirectoryAtPath(folder, withIntermediateDirectories = true, attributes = null, error = null)
    return "$folder/settings.sts"
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun readSettingsText(): String? =
    if (NSFileManager.defaultManager.fileExistsAtPath(settingsPath())) NSString.stringWithContentsOfFile(settingsPath(), NSUTF8StringEncoding, null) else null

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun writeSettingsText(text: String) {
    NSString.create(string = text).writeToFile(settingsPath(), atomically = true, encoding = NSUTF8StringEncoding, error = null)
}
