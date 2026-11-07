package org.mr0btain.tunerstoolkit.components.datastorage.components
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData

fun getSettings(): SettingsData { return ProjectFilesManager().getSettings() }

fun saveSettings(settings: SettingsData) { ProjectFilesManager().saveSettings(settings) }