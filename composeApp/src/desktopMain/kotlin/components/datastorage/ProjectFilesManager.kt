@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package components.datastorage

import components.datastorage.components.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import screens.settings.classes.SettingsData
import java.io.File

actual class ProjectFilesManager actual constructor() {
    // Used to get the local directory where app files are stored
    private val directoryResolver = DirectoryResolverFactory().instance

    private val projectDirectory =
        File("${directoryResolver.getUserDataDir("TunersToolkit", false)}/projects")

    actual fun getProjectList(): List<String> {
        return projectDirectory.list()?.toList() ?: listOf()
    }

    actual fun getProjectVersionsList(name: String): List<String> {
        return File("${projectDirectory.absolutePath}/$name").list()?.toList() ?: listOf()
    }

    actual fun getProjectData(name: String, version: String): List<Pair<ProjectFileType, ByteArray>> {
        val projects = getProjectList()
        val result = mutableListOf<Pair<ProjectFileType, ByteArray>>()

        if (projects.contains(name)) {
            val projectDir = File("${projectDirectory.absolutePath}/$name/$version")

            projectDir.list()?.toList()?.forEach { item ->
                val type = item.getFileType()

                if (type != ProjectFileType.NotFound) {
                    val file = File("${projectDir.absolutePath}/$item").readBytes()
                    result.add(Pair(type, file))
                }
            }
        }

        return  result
    }

    actual fun removeEntireProject(name: String) {
        File("${projectDirectory.absolutePath}/$name").deleteRecursively()
    }

    actual fun removeProjectVersion(name: String, version: String) {
        File("${projectDirectory.absolutePath}/$name/$version").deleteRecursively()
    }

    actual fun addNewProject(name: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        // Make the File for our directory
        val projectDir = File("${projectDirectory.absolutePath}/$name/original")

        if (!projectDir.isDirectory) {
            // Make Directory if it doesn't exist
            projectDir.mkdirs()
        }

        // Write the files
        data.forEach {
            val file = File("${projectDir.absolutePath}/${it.first.name}")
            file.createNewFile()
            val output = file.outputStream()
            output.write(it.second)
            output.close()
        }
    }

    actual fun addProjectVersion(name: String, version: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        // Make the File for our directory
        val projectDir = File("${projectDirectory.absolutePath}/$name/$version")

        if (!projectDir.isDirectory) {
            // Make Directory if it doesn't exist
            projectDir.mkdirs()
        }

        // Write the files
        data.forEach {
            val file = File("${projectDir.absolutePath}/${it.first.name}")
            file.createNewFile()
            val output = file.outputStream()
            output.write(it.second)
            output.close()
        }
    }

    actual fun saveChanges(name: String, version: String, data: ProjectChanges) {
        // TODO: Not yet implemented
    }

    actual fun getChanges(name: String, version: String): ProjectChanges {
        // TODO: Not yet Implemented
        return ProjectChanges(mapOf(), mapOf())
    }

    actual fun getSettings(): SettingsData {
        val dir = File("${projectDirectory.absolutePath}/settings.sts")
        if (dir.exists()) {
            val input = dir.inputStream()
            val data = input.readBytes().decodeToString()
            input.close()
            return Json.decodeFromString<SettingsData>(data)
        }

        return SettingsData()
    }

    actual fun saveSettings(settings: SettingsData) {
        var dir = File(projectDirectory.absolutePath)
        if (!dir.exists()) { dir.mkdirs() }

        dir = File("${projectDirectory.absolutePath}/settings.sts")
        dir.createNewFile()

        val output = dir.outputStream()
        output.write(Json.encodeToString(settings).encodeToByteArray())
    }
}