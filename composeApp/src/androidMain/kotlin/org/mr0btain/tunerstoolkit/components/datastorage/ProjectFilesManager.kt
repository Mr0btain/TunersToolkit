package org.mr0btain.tunerstoolkit.components.datastorage
import android.content.Context
import org.mr0btain.tunerstoolkit.appContext
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectChanges
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectFileType
import org.mr0btain.tunerstoolkit.components.datastorage.components.getFileType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData
import java.io.File

actual class ProjectFilesManager actual constructor() {
    private val mContext = appContext
    private val mProjectDir = mContext.getDir("projects", Context.MODE_PRIVATE)

    actual fun getProjectVersionsList(name: String): List<String> {
        val projectDir = File("${mProjectDir.absolutePath}/$name")
        return projectDir.list()?.toList() ?: listOf()
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
        val projectDir = File("${mProjectDir.absolutePath}/$name")
        projectDir.deleteRecursively()
    }

    actual fun removeProjectVersion(name: String, version: String) {
        val projectVersionDir = File("${mProjectDir.absolutePath}/$name/$version")
        projectVersionDir.deleteRecursively()
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

    actual fun getProjectList(): List<String> { return mProjectDir.list()?.toList() ?: listOf() }

    actual fun saveChanges(name: String, version: String, data: ProjectChanges) {

    }

    actual fun getChanges(name: String, version: String): ProjectChanges {
        return ProjectChanges(mapOf(), mapOf())
    }

    actual fun getSettings(): SettingsData {
        val mDir = File("${mProjectDir.absolutePath}/settings.sts")
        return if (mDir.exists()) {
            val input = mDir.inputStream()
            val data = input.readBytes().decodeToString()
            input.close()
            Json.decodeFromString<SettingsData>(data)
        } else SettingsData()


    }

    actual fun saveSettings(settings: SettingsData) {
        val mDir = File("${mProjectDir.absolutePath}/settings.sts")

        mDir.createNewFile()
        val output = mDir.outputStream()
        output.write(Json.encodeToString(settings).encodeToByteArray())
    }
}