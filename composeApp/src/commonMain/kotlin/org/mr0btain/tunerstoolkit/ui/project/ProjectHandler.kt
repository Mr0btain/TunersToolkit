package org.mr0btain.tunerstoolkit.ui.project

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.project.ProjectError
import org.mr0btain.tunerstoolkit.core.project.ProjectInfo
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.core.project.ProjectSource
import org.mr0btain.tunerstoolkit.core.project.ProjectVersion
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler

/**
 * The project the open image belongs to, if any, and which of its versions
 */
data class OpenProject(val manifest: ProjectManifest, val versionId: String) {
    val version: ProjectVersion? get() = manifest.version(versionId)
    val isOriginal: Boolean get() = versionId == ProjectManifest.ORIGINAL
    val versionName: String get() = version?.name ?: "Original"
}

/**
 * Saved projects, and saving the open image into them. Every open goes through here (not straight to
 * [DocumentHandler.open]), so [current] always matches what's in the editor.
 * @param notify Shows a short message, like "Saved"
 * @param readInBackground Reads images on a background thread; false for tests and previews
 */
@Stable
class ProjectHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val repository: ProjectRepository = ProjectRepository(platformProjectStore()),
    private val notify: (String) -> Unit = {},
    private val readInBackground: Boolean = true
) {
    var projects by mutableStateOf<List<ProjectManifest>>(emptyList())
        private set

    /** The open image's project, null for a file that hasn't been saved as one */
    var current by mutableStateOf<OpenProject?>(null)
        private set

    /** Saving, or reading a project */
    var busy by mutableStateOf(false)
        private set

    /** The document version when it was opened or last saved, for [hasUnsavedChanges] */
    private var savedAt by mutableLongStateOf(Long.MIN_VALUE)

    /** Whether projects outlive the app here; not in a browser without OPFS */
    val persistent: Boolean get() = repository.persistent

    /** Whether the open image has been edited (or undone) since it was opened or saved */
    val hasUnsavedChanges: Boolean
        get() = documents.document != null && documents.version != savedAt

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            when (val result = repository.list()) {
                is TkResult.Success -> projects = result.data
                is TkResult.Failure -> notify(result.error.message)
            }
        }
    }

    /** Opens a file that isn't in a project (yet) */
    fun openFile(bytes: ByteArray, name: String, rawBaseAddress: Long = 0, onOpened: () -> Unit = {}) {
        documents.open(bytes, name, rawBaseAddress, inBackground = readInBackground) {
            current = null
            savedAt = documents.version
            onOpened()
        }
    }

    /** Opens [versionId] of [manifest]: its original file with that version's changes on it */
    fun openProject(manifest: ProjectManifest, versionId: String = manifest.latest?.id ?: ProjectManifest.ORIGINAL, onOpened: () -> Unit = {}) {
        scope.launch {
            busy = true
            val source = repository.source(manifest.id)
            val patches = repository.patches(manifest.id, versionId)
            busy = false
            if (source !is TkResult.Success) return@launch failed("Open", (source as TkResult.Failure).error)
            if (patches !is TkResult.Success) return@launch failed("Open", (patches as TkResult.Failure).error)
            documents.open(source.data, manifest.source.fileName, manifest.source.rawBaseAddress, patches.data, readInBackground) {
                current = OpenProject(manifest, versionId)
                savedAt = documents.version
                onOpened()
            }
        }
    }

    /**
     * Makes a project of the open file. Changes already made become its first version, named [versionName]
     */
    fun saveAsProject(name: String, versionName: String, note: String, info: ProjectInfo = ProjectInfo()) {
        val document = documents.document ?: return
        val patches = document.image.patchesFromOriginal()
        launchSaving("SaveAsProject") {
            val source = ProjectSource(document.name, document.format.name, document.source.size.toLong(), document.rawBaseAddress)
            val manifest = when (val created = repository.create(name, source, document.source, info)) {
                is TkResult.Success -> created.data
                is TkResult.Failure -> return@launchSaving created.error
            }
            current = OpenProject(manifest, ProjectManifest.ORIGINAL)
            if (patches.isNotEmpty()) {
                when (val added = repository.addVersion(manifest.id, versionName, patches, note)) {
                    is TkResult.Success -> current = OpenProject(added.data.first, added.data.second.id)
                    is TkResult.Failure -> return@launchSaving added.error
                }
            }
            "Saved as ${manifest.name}"
        }
    }

    /** Saves the open version in place. The original can't be, so use [saveAsVersion] for that */
    fun save() {
        val open = current ?: return
        if (open.isOriginal) return
        val patches = documents.document?.image?.patchesFromOriginal() ?: return
        launchSaving("Save") {
            when (val result = repository.updateVersion(open.manifest.id, open.versionId, patches)) {
                is TkResult.Success -> {
                    current = open.copy(manifest = result.data)
                    "Saved ${open.versionName}"
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    /** Saves the open image as a new version of its project, and carries on editing that */
    fun saveAsVersion(name: String, note: String) {
        val open = current ?: return
        val patches = documents.document?.image?.patchesFromOriginal() ?: return
        launchSaving("SaveAsVersion") {
            when (val result = repository.addVersion(open.manifest.id, name, patches, note, basedOn = open.versionId)) {
                is TkResult.Success -> {
                    val (manifest, version) = result.data
                    current = OpenProject(manifest, version.id)
                    "Saved ${version.name}"
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    fun rename(id: String, name: String, info: ProjectInfo) {
        launchSaving("Rename", markSaved = false) {
            when (val result = repository.update(id) { it.copy(name = name.trim().ifEmpty { it.name }, info = info) }) {
                is TkResult.Success -> {
                    current?.takeIf { it.manifest.id == id }?.let { current = it.copy(manifest = result.data) }
                    null
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    fun deleteVersion(id: String, versionId: String) {
        launchSaving("DeleteVersion", markSaved = false) {
            when (val result = repository.deleteVersion(id, versionId)) {
                is TkResult.Success -> {
                    // The editor keeps the bytes; they just aren't that version any more
                    current?.takeIf { it.manifest.id == id }?.let {
                        current = if (it.versionId == versionId) null else it.copy(manifest = result.data)
                    }
                    "Version deleted"
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    fun deleteProject(id: String) {
        launchSaving("DeleteProject", markSaved = false) {
            when (val result = repository.delete(id)) {
                is TkResult.Success -> {
                    if (current?.manifest?.id == id) current = null
                    "Project deleted"
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    /**
     * Runs a save, then refreshes the list and says how it went
     * @param block Returns a message, a [ProjectError] when it failed, or null to say nothing
     * @param markSaved Whether success means the open image is saved
     */
    private fun launchSaving(function: String, markSaved: Boolean = true, block: suspend () -> Any?) {
        val versionAtStart = documents.version
        scope.launch {
            busy = true
            val outcome = block()
            busy = false
            when (outcome) {
                is ProjectError -> failed(function, outcome)
                else -> {
                    if (markSaved) savedAt = versionAtStart
                    (outcome as? String)?.let(notify)
                }
            }
            refresh()
        }
    }

    private fun failed(function: String, error: ProjectError) {
        AppLogger.w("$TAG:$function", error.message)
        notify(error.message)
    }

    private companion object {
        const val TAG = "ProjectHandler"
    }
}
