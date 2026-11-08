package org.mr0btain.tunerstoolkit.ui.settings

import android.content.Context
import org.mr0btain.tunerstoolkit.appContext
import java.io.File

/** Where settings have always been on Android: the app's projects folder */
private fun settingsFile(): File = File(appContext.getDir("projects", Context.MODE_PRIVATE), "settings.sts")

internal actual fun readSettingsText(): String? = settingsFile().takeIf { it.isFile }?.readText()

internal actual fun writeSettingsText(text: String) = settingsFile().writeText(text)
