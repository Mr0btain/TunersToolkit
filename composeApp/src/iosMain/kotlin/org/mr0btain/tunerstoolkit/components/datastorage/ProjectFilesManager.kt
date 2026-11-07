package org.mr0btain.tunerstoolkit.components.datastorage
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectChanges
import org.mr0btain.tunerstoolkit.components.datastorage.components.ProjectFileType
import org.mr0btain.tunerstoolkit.components.datastorage.components.getFileType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSDirectoryEnumerationSkipsPackageDescendants
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.pathExtension
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData

typealias ImageBytes = NSData

@OptIn(ExperimentalForeignApi::class)
actual class ProjectFilesManager actual constructor() {
    private val mFileManager = NSFileManager.defaultManager
    // private val mTmpDir = mFileManager.temporaryDirectory

    private val mAppFilesDir = mFileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )

    private val mProjectDir = NSURL(fileURLWithPath = "${mAppFilesDir?.path}/projects/")

    actual fun getProjectList(): List<String> {
        val mResult = mFileManager.contentsOfDirectoryAtURL(
            url = mProjectDir,
            includingPropertiesForKeys = null,
            options = NSDirectoryEnumerationSkipsPackageDescendants,
            error = null
        )

        return if (mResult == null) {
            mFileManager.createDirectoryAtURL(
                url = mProjectDir,
                withIntermediateDirectories = true,
                error = null,
                attributes = null
            )
            listOf()
        } else mResult.map { it.toString() }
    }

    actual fun getProjectVersionsList(name: String): List<String> {
        val mResult = mFileManager.contentsOfDirectoryAtURL(
            url = NSURL(fileURLWithPath = "${mProjectDir.relativePath}/$name/"),
            includingPropertiesForKeys = null,
            options = NSDirectoryEnumerationSkipsPackageDescendants,
            error = null
        )

        return mResult?.map { it.toString() } ?: listOf()
    }

    actual fun getProjectData(name: String, version: String): List<Pair<ProjectFileType, ByteArray>> {
        val url = NSURL.fileURLWithPath("${mProjectDir.path}/$name/$version/")
        val mResult = mutableListOf<Pair<ProjectFileType, ByteArray>>()

        url.path?.let {
            mFileManager.contentsOfDirectoryAtPath(it, error = null)?.forEach { path ->
                if (path is NSString) {
                    val data = mFileManager.contentsAtPath(path = path.pathExtension)

                    if (data != null) {
                        data.description?.let { it1 -> mResult.add(Pair(it1.getFileType(), data.toByteArray())) }
                    }
                }
            }
        }

        return mResult
    }

    actual fun removeEntireProject(name: String) {
        mFileManager.removeItemAtURL(NSURL(fileURLWithPath = "${mProjectDir.path}/$name"), error = null)
    }

    actual fun removeProjectVersion(name: String, version: String) {
        mFileManager.removeItemAtURL(NSURL(fileURLWithPath = "${mProjectDir.path}/$name/$version"), error = null)
    }

    actual fun addNewProject(name: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        mProjectDir.path?.let {
            // If main project directory does not exist, make it
            if (!mFileManager.fileExistsAtPath(it) ) {
                if (mAppFilesDir != null) {
                    mFileManager.createDirectoryAtURL(
                        url = mAppFilesDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }
            }

            val mProjectUrl = NSURL(fileURLWithPath = "${mProjectDir.path!!}/$name/")
            // Create the directory
            mFileManager.createDirectoryAtURL(
                url = mProjectUrl,
                attributes = null,
                withIntermediateDirectories = true,
                error = null
            )

            data.forEach { file ->
                mFileManager.createFileAtPath(
                    path = "${mProjectUrl.path}/${file.first.name}",
                    attributes = null,
                    contents = file.second.toImageBytes()
                )
            }
        }
    }

    actual fun addProjectVersion(name: String, version: String, data: List<Pair<ProjectFileType, ByteArray>>) {
        mProjectDir.path?.let {
            // If main project directory does not exist, make it
            if (!mFileManager.fileExistsAtPath(it) ) {
                if (mAppFilesDir != null) {
                    mFileManager.createDirectoryAtURL(
                        url = mAppFilesDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }
            }

            val mProjectUrl = NSURL(fileURLWithPath = "${mProjectDir.path!!}/$name/$version/")
            // Create the directory
            mFileManager.createDirectoryAtURL(
                url = mProjectUrl,
                attributes = null,
                withIntermediateDirectories = true,
                error = null
            )

            data.forEach { file ->
                mFileManager.createFileAtPath(
                    path = "${mProjectUrl.path}/${file.first.name}",
                    attributes = null,
                    contents = file.second.toImageBytes()
                )
            }
        }
    }

    private fun NSData.toByteArray(): ByteArray { return this.toByteArray() }

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toImageBytes(): ImageBytes = memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toImageBytes),
            length = this@toImageBytes.size.toULong()
        )
    }

    actual fun saveChanges(name: String, version: String, data: ProjectChanges) {

    }

    actual fun getChanges(name: String, version: String): ProjectChanges {
        return ProjectChanges(mapOf(), mapOf())
    }

    actual fun getSettings(): SettingsData {
        val mSettingsData = NSURL(fileURLWithPath = "${mProjectDir.path!!}/settings.sts")

        return if (mFileManager.fileExistsAtPath(mSettingsData.path.toString())) {
            val data = mFileManager.contentsAtPath(mSettingsData.path.toString())?.toByteArray()

            data?.decodeToString()?.let { Json.decodeFromString<SettingsData>(it) } ?: SettingsData()
        } else SettingsData()
    }

    actual fun saveSettings(settings: SettingsData) {
        val mSettingsData = NSURL(fileURLWithPath = "${mProjectDir.path!!}/settings.sts")

        mFileManager.createFileAtPath(
            path = mSettingsData.path.toString(),
            attributes = null,
            contents = Json.encodeToString(settings).encodeToByteArray().toImageBytes()
        )
    }
}