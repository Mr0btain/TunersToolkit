package org.mr0btain.tunerstoolkit.ui.settings

import kotlinx.browser.localStorage

private const val SETTINGS_KEY = "tunerstoolkit.settings"

internal actual fun readSettingsText(): String? = localStorage.getItem(SETTINGS_KEY)

internal actual fun writeSettingsText(text: String) = localStorage.setItem(SETTINGS_KEY, text)
