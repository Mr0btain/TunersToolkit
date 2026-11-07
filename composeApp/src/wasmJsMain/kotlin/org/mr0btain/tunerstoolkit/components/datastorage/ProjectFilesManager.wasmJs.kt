package org.mr0btain.tunerstoolkit.components.datastorage

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectChanges
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectFileType
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData

private const val TAG = "ProjectFilesManager"
private const val SETTINGS_KEY = "tunerstoolkit.settings"

/**
 * Projects only last while the page is open in the browser for now; saving them in the Origin Private File System
 * is in docs/PLAN.md, Phase 2. Settings are kept in localStorage
 */
actual class ProjectFilesManager actual constructor() {
    actual fun getProjectList(): List<String> = projects.keys.toList()

    actual fun getProjectVersionsList(name: String): List<String> = projects[name]?.keys?.toList() ?: listOf()

    actual fun getProjectData(name: String, version: String): List<Pair<ProjectFileType, ByteArray>> =
        projects[name]?.get(version) ?: listOf()

    actual fun removeEntireProject(name: String) {
        projects.remove(name)
    }

    actual fun removeProjectVersion(name: String, version: String) {
        projects[name]?.remove(version)
    }

    actual fun addNewProject(name: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        AppLogger.w("$TAG:AddNewProject", "Projects aren't saved between visits on the web yet")
        projects[name] = mutableMapOf("original" to data)
    }

    actual fun addProjectVersion(name: String, version: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        projects.getOrPut(name) { mutableMapOf() }[version] = data
    }

    actual fun saveChanges(name: String, version: String, data: ProjectChanges) {
        // Not implemented on any platform yet
    }

    actual fun getChanges(name: String, version: String): ProjectChanges = ProjectChanges(mapOf(), mapOf())

    actual fun getSettings(): SettingsData = try {
        localStorage.getItem(SETTINGS_KEY)?.let { Json.decodeFromString<SettingsData>(it) } ?: SettingsData()
    } catch (e: Exception) {
        AppLogger.w("$TAG:GetSettings", "Saved settings couldn't be read, using the defaults", e)
        SettingsData()
    }

    actual fun saveSettings(settings: SettingsData) {
        localStorage.setItem(SETTINGS_KEY, Json.encodeToString(settings))
    }

    private companion object {
        val projects = mutableMapOf<String, MutableMap<String, List<Pair<ProjectFileType, ByteArray>>>>()
    }
}
