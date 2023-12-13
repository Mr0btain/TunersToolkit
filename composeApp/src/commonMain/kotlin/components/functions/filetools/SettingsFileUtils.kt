package components.functions.filetools

import components.datastorage.ProjectFilesManager
import screens.settings.classes.SettingsData

fun getSettings(): SettingsData { return ProjectFilesManager().getSettings() }

fun saveSettings(settings: SettingsData) { ProjectFilesManager().saveSettings(settings) }