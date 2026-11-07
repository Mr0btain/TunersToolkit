@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.mr0btain.tunerstoolkit.components.datastorage
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectChanges
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectFileType
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData


expect class ProjectFilesManager() {
    fun getProjectList(): List<String>
    fun getProjectVersionsList(name: String): List<String>
    fun getProjectData(name: String, version: String): List<Pair<ProjectFileType, ByteArray>>
    fun removeEntireProject(name: String)
    fun removeProjectVersion(name: String, version: String)
    fun addNewProject(name: String, data: List<Pair<ProjectFileType, ByteArray>>)
    fun addProjectVersion(name: String, version: String, data: List<Pair<ProjectFileType, ByteArray>>)
    fun saveChanges(name: String, version: String, data: ProjectChanges)
    fun getChanges(name: String, version: String): ProjectChanges
    fun getSettings(): SettingsData
    fun saveSettings(settings: SettingsData)
}