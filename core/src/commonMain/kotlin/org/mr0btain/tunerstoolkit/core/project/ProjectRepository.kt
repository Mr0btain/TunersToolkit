package org.mr0btain.tunerstoolkit.core.project

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.Patch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "ProjectRepository"

/**
 * Projects and their versions in a [ProjectStore]. Everything here returns a [TkResult] rather than throwing, and
 * logs what failed (names and sizes only, never file contents).
 * @param now The time in milliseconds since 1970, for the timestamps
 * @param newId A fresh id for a project or version
 */
class ProjectRepository(
    private val store: ProjectStore,
    private val now: () -> Long = ::currentMillis,
    private val newId: () -> String = ::randomId
) {
    /** Whether projects outlive the app, see [ProjectStore.persistent] */
    val persistent: Boolean get() = store.persistent

    /** Every project that can be read, most recently changed first. Ones that can't be are logged and left out */
    suspend fun list(): TkResult<List<ProjectManifest>, ProjectError> = guard("List", ProjectError.UNREADABLE) {
        val manifests = store.list(PROJECTS).mapNotNull { id ->
            when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> {
                    AppLogger.w("$TAG:List", "Skipped project $id: ${result.error.message}")
                    null
                }
            }
        }
        TkResult.Success(manifests.sortedByDescending { it.updatedAt })
    }

    suspend fun manifest(id: String): TkResult<ProjectManifest, ProjectError> = guard("Manifest", ProjectError.UNREADABLE) {
        val bytes = store.read(manifestPath(id)) ?: return@guard TkResult.Failure(ProjectError.NOT_FOUND)
        val manifest = json.decodeFromString<ProjectManifest>(bytes.decodeToString())
        if (manifest.formatVersion > ProjectManifest.FORMAT_VERSION) TkResult.Failure(ProjectError.NEWER_FORMAT)
        else TkResult.Success(manifest)
    }

    /**
     * A new project around the file [sourceBytes], with only the original in it
     */
    suspend fun create(
        name: String,
        source: ProjectSource,
        sourceBytes: ByteArray,
        info: ProjectInfo = ProjectInfo()
    ): TkResult<ProjectManifest, ProjectError> = guard("Create", ProjectError.SAVE_FAILED) {
        val time = now()
        val manifest = ProjectManifest(
            id = newId(),
            name = name.trim().ifEmpty { source.fileName },
            createdAt = time,
            updatedAt = time,
            source = source.copy(size = sourceBytes.size.toLong()),
            info = info
        )
        store.write(sourcePath(manifest.id), sourceBytes)
        writeManifest(manifest)
        AppLogger.i("$TAG:Create", "Created project ${manifest.id} from ${source.fileName}, ${sourceBytes.size} bytes")
        TkResult.Success(manifest)
    }

    /** The file the project was made from, exactly as it was */
    suspend fun source(id: String): TkResult<ByteArray, ProjectError> = guard("Source", ProjectError.UNREADABLE) {
        store.read(sourcePath(id))?.let { TkResult.Success(it) } ?: TkResult.Failure(ProjectError.NOT_FOUND)
    }

    /**
     * Saves [bytes] as the project's definition, replacing any it had
     */
    suspend fun setDefinition(id: String, fileName: String, bytes: ByteArray): TkResult<ProjectManifest, ProjectError> =
        guard("SetDefinition", ProjectError.SAVE_FAILED) {
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            store.write(definitionPath(id), bytes)
            val updated = manifest.copy(updatedAt = now(), definition = ProjectDefinition(fileName, bytes.size.toLong()))
            writeManifest(updated)
            AppLogger.i("$TAG:SetDefinition", "Saved a definition for $id from $fileName, ${bytes.size} bytes")
            TkResult.Success(updated)
        }

    /** Saves maps made by hand with the project (a .ttdef), or removes them when [bytes] is null */
    suspend fun setAdditions(id: String, bytes: ByteArray?): TkResult<ProjectManifest, ProjectError> =
        guard("SetAdditions", ProjectError.SAVE_FAILED) {
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            if (bytes == null) store.delete(additionsPath(id)) else store.write(additionsPath(id), bytes)
            val updated = manifest.copy(updatedAt = now(), additions = bytes?.let { ProjectDefinition(ADDITIONS, it.size.toLong()) })
            writeManifest(updated)
            TkResult.Success(updated)
        }

    /**
     * Saves a file of [name] with the project (`files/<name>`), or removes it when [bytes] is null. Names are
     * letters, digits, dots, dashes and underscores, so they can't reach outside the project's folder
     */
    suspend fun setFile(id: String, name: String, bytes: ByteArray?): TkResult<ProjectManifest, ProjectError> =
        guard("SetFile", ProjectError.SAVE_FAILED) {
            require(isFileName(name)) { "Not a file name" }
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            if (bytes == null) store.delete(filePath(id, name)) else store.write(filePath(id, name), bytes)
            val files = if (bytes == null) manifest.files - name else manifest.files + (name to bytes.size.toLong())
            val updated = manifest.copy(updatedAt = now(), files = files)
            writeManifest(updated)
            TkResult.Success(updated)
        }

    /** A file saved with the project, or null when there's none of that name */
    suspend fun file(id: String, name: String): TkResult<ByteArray?, ProjectError> = guard("File", ProjectError.UNREADABLE) {
        if (!isFileName(name)) return@guard TkResult.Success(null)
        TkResult.Success(store.read(filePath(id, name)))
    }

    /** The project's maps made by hand, or null when it has none */
    suspend fun additions(id: String): TkResult<ByteArray?, ProjectError> = guard("Additions", ProjectError.UNREADABLE) {
        TkResult.Success(store.read(additionsPath(id)))
    }

    /** The project's definition file, or null when it has none */
    suspend fun definition(id: String): TkResult<ByteArray?, ProjectError> = guard("Definition", ProjectError.UNREADABLE) {
        TkResult.Success(store.read(definitionPath(id)))
    }

    /** A version's changes from the original; none for [ProjectManifest.ORIGINAL] */
    suspend fun patches(id: String, versionId: String): TkResult<List<Patch>, ProjectError> = guard("Patches", ProjectError.UNREADABLE) {
        if (versionId == ProjectManifest.ORIGINAL) return@guard TkResult.Success(emptyList())
        val bytes = store.read(versionPath(id, versionId)) ?: return@guard TkResult.Failure(ProjectError.VERSION_NOT_FOUND)
        val saved = json.decodeFromString<VersionPatches>(bytes.decodeToString())
        if (saved.formatVersion > ProjectManifest.FORMAT_VERSION) TkResult.Failure(ProjectError.NEWER_FORMAT)
        else TkResult.Success(saved.patches.map { it.toPatch() })
    }

    /**
     * Saves [patches] (changes from the original, as MemoryImage.patchesFromOriginal gives) as a new version
     * @return The updated project and the new version
     */
    suspend fun addVersion(
        id: String,
        name: String,
        patches: List<Patch>,
        note: String = "",
        basedOn: String = ProjectManifest.ORIGINAL
    ): TkResult<Pair<ProjectManifest, ProjectVersion>, ProjectError> = guard("AddVersion", ProjectError.SAVE_FAILED) {
        val manifest = when (val result = manifest(id)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> return@guard result
        }
        val time = now()
        val version = ProjectVersion(
            id = newId(),
            name = name.trim().ifEmpty { "Version ${manifest.versions.size + 1}" },
            createdAt = time,
            updatedAt = time,
            note = note,
            basedOn = basedOn,
            changedBytes = patches.changedBytes()
        )
        writePatches(id, version.id, patches)
        val updated = manifest.copy(updatedAt = time, versions = manifest.versions + version)
        writeManifest(updated)
        AppLogger.i("$TAG:AddVersion", "Saved version ${version.id} of $id, ${version.changedBytes} bytes changed")
        TkResult.Success(updated to version)
    }

    /** Replaces a saved version's changes with [patches]. The original can't be changed */
    suspend fun updateVersion(id: String, versionId: String, patches: List<Patch>): TkResult<ProjectManifest, ProjectError> =
        guard("UpdateVersion", ProjectError.SAVE_FAILED) {
            if (versionId == ProjectManifest.ORIGINAL) return@guard TkResult.Failure(ProjectError.READ_ONLY)
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            if (manifest.version(versionId) == null) return@guard TkResult.Failure(ProjectError.VERSION_NOT_FOUND)
            val time = now()
            writePatches(id, versionId, patches)
            val updated = manifest.copy(
                updatedAt = time,
                versions = manifest.versions.map {
                    if (it.id == versionId) it.copy(updatedAt = time, changedBytes = patches.changedBytes()) else it
                }
            )
            writeManifest(updated)
            AppLogger.i("$TAG:UpdateVersion", "Saved version $versionId of $id, ${patches.changedBytes()} bytes changed")
            TkResult.Success(updated)
        }

    /** Changes a project's name or info; the versions are left alone */
    suspend fun update(id: String, change: (ProjectManifest) -> ProjectManifest): TkResult<ProjectManifest, ProjectError> =
        guard("Update", ProjectError.SAVE_FAILED) {
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            val updated = change(manifest).copy(id = manifest.id, versions = manifest.versions, updatedAt = now())
            writeManifest(updated)
            TkResult.Success(updated)
        }

    /** Renames a version or changes its note. ([update] keeps the versions as they were, so this writes its own) */
    suspend fun updateVersionInfo(id: String, versionId: String, name: String, note: String): TkResult<ProjectManifest, ProjectError> =
        guard("UpdateVersionInfo", ProjectError.SAVE_FAILED) {
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            if (manifest.version(versionId) == null) return@guard TkResult.Failure(ProjectError.VERSION_NOT_FOUND)
            val updated = manifest.copy(
                updatedAt = now(),
                versions = manifest.versions.map { if (it.id == versionId) it.copy(name = name.trim().ifEmpty { it.name }, note = note) else it }
            )
            writeManifest(updated)
            TkResult.Success(updated)
        }

    suspend fun deleteVersion(id: String, versionId: String): TkResult<ProjectManifest, ProjectError> =
        guard("DeleteVersion", ProjectError.SAVE_FAILED) {
            if (versionId == ProjectManifest.ORIGINAL) return@guard TkResult.Failure(ProjectError.READ_ONLY)
            val manifest = when (val result = manifest(id)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@guard result
            }
            val updated = manifest.copy(updatedAt = now(), versions = manifest.versions.filterNot { it.id == versionId })
            writeManifest(updated)
            store.delete(versionPath(id, versionId))
            AppLogger.i("$TAG:DeleteVersion", "Deleted version $versionId of $id")
            TkResult.Success(updated)
        }

    suspend fun delete(id: String): TkResult<Unit, ProjectError> = guard("Delete", ProjectError.SAVE_FAILED) {
        store.delete("$PROJECTS/$id")
        AppLogger.i("$TAG:Delete", "Deleted project $id")
        TkResult.Success(Unit)
    }

    private suspend fun writeManifest(manifest: ProjectManifest) {
        store.write(manifestPath(manifest.id), json.encodeToString(ProjectManifest.serializer(), manifest).encodeToByteArray())
    }

    private suspend fun writePatches(id: String, versionId: String, patches: List<Patch>) {
        val saved = VersionPatches(patches.map(SavedPatch::of))
        store.write(versionPath(id, versionId), json.encodeToString(VersionPatches.serializer(), saved).encodeToByteArray())
    }

    /** Runs [block], turning anything the store or the JSON throws into [error] */
    private inline fun <T> guard(
        function: String,
        error: ProjectError,
        block: () -> TkResult<T, ProjectError>
    ): TkResult<T, ProjectError> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.e("$TAG:$function", error.message, e)
        TkResult.Failure(error)
    }

    companion object {
        const val PROJECTS = "projects"

        fun manifestPath(id: String) = "$PROJECTS/$id/project.json"
        fun sourcePath(id: String) = "$PROJECTS/$id/source.bin"
        fun versionPath(id: String, versionId: String) = "$PROJECTS/$id/versions/$versionId.json"
        fun definitionPath(id: String) = "$PROJECTS/$id/definition.bin"
        fun additionsPath(id: String) = "$PROJECTS/$id/$ADDITIONS"
        const val ADDITIONS = "additions.ttdef"
        fun filePath(id: String, name: String) = "$PROJECTS/$id/files/$name"
        fun isFileName(name: String) = name.isNotEmpty() && name.length <= 100 && name.first() != '.' && name.all { it.isLetterOrDigit() || it in "._-" }

        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }
    }
}

private fun List<Patch>.changedBytes(): Long = sumOf { patch ->
    val before = patch.before
    val after = patch.after
    before.indices.count { before[it] != after[it] }.toLong()
}

@OptIn(ExperimentalTime::class)
private fun currentMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** 16 hex digits: unique enough for one person's projects, and safe as a file name everywhere */
@OptIn(ExperimentalStdlibApi::class)
private fun randomId(): String = Random.nextLong().toHexString()
