@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package components.datastorage

import components.datastorage.components.ProjectChanges
import components.datastorage.components.ProjectFileType
import components.datastorage.components.getFileType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.harawata.appdirs.AppDirsFactory
import screens.settings.classes.SettingsData
import java.io.File

actual class ProjectFilesManager actual constructor() {
    // Used to get the local directory where app files are stored
    private val appDirs = AppDirsFactory.getInstance()
    private val mProjectDir =
        File("${appDirs.getUserDataDir("TunersToolkit", null, "")}/projects")

    actual fun getProjectList(): List<String> {
        return mProjectDir.list()?.toList() ?: listOf()
    }

    actual fun getProjectVersionsList(name: String): List<String> {
        return File("${mProjectDir.absolutePath}/$name").list()?.toList() ?: listOf()
    }

    actual fun getProjectData(name: String, version: String): List<Pair<ProjectFileType, ByteArray>> {
        val projects = getProjectList()
        val mDataResult = mutableListOf<Pair<ProjectFileType, ByteArray>>()

        if (projects.contains(name)) {
            val projectDir = File("${mProjectDir.absolutePath}/$name/$version")

            projectDir.list()?.toList()?.forEach { item ->
                val type = item.getFileType()

                if (type != ProjectFileType.NotFound) {
                    val file = File("${projectDir.absolutePath}/$item").readBytes()
                    mDataResult.add(Pair(type, file))
                }
            }
        }

        return  mDataResult
    }

    actual fun removeEntireProject(name: String) {
        File("${mProjectDir.absolutePath}/$name").deleteRecursively()
    }

    actual fun removeProjectVersion(name: String, version: String) {
        File("${mProjectDir.absolutePath}/$name/$version").deleteRecursively()
    }

    actual fun addNewProject(name: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        // Make the File for our directory
        val projectDir = File("${mProjectDir.absolutePath}/$name/original")

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
        val projectDir = File("${mProjectDir.absolutePath}/$name/$version")

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

    }

    actual fun getChanges(name: String, version: String): ProjectChanges {
        return ProjectChanges(mapOf(), mapOf())
    }

    actual fun getSettings(): SettingsData {
        val mDir = File("${mProjectDir.absolutePath}/settings.sts")
        if (mDir.exists()) {
            val input = mDir.inputStream()
            val data = input.readBytes().decodeToString()
            input.close()
            return Json.decodeFromString<SettingsData>(data)
        }

        return SettingsData()
    }

    actual fun saveSettings(settings: SettingsData) {
        var mDir = File(mProjectDir.absolutePath)
        if (!mDir.exists()) { mDir.mkdirs() }

        mDir = File("${mProjectDir.absolutePath}/settings.sts")
        mDir.createNewFile()

        val output = mDir.outputStream()
        output.write(Json.encodeToString(settings).encodeToByteArray())
    }
}