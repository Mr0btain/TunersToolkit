package org.mr0btain.tunerstoolkit.ui.settings

import kotlinx.serialization.json.Json

/** The saved settings file's text, or null when there isn't one. Where it is depends on the platform */
internal expect fun readSettingsText(): String?

internal expect fun writeSettingsText(text: String)

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** The saved settings; throws when they're there but can't be read */
internal fun loadSettingsData(): SettingsData = readSettingsText()?.let { json.decodeFromString<SettingsData>(it) } ?: SettingsData()

internal fun saveSettingsData(settings: SettingsData) = writeSettingsText(json.encodeToString(SettingsData.serializer(), settings))
