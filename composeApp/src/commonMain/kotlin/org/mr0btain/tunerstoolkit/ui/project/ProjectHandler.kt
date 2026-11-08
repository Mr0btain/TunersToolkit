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
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryFormat
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryImport
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.project.ProjectError
import org.mr0btain.tunerstoolkit.core.project.ProjectInfo
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.core.project.ProjectSource
import org.mr0btain.tunerstoolkit.core.project.ProjectVersion
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler

/**
 * Something saved with a project as a file of its own (a checksum module, say): the project handler saves it when it's
 * changed, gives it back when a project opens, and counts it in [ProjectHandler.hasUnsavedChanges]
 */
interface ProjectAttachment {
    /** Its file's name in the project: letters, digits, dots, dashes and underscores */
    val fileName: String

    /** What to save now, or null for nothing (the file is removed) */
    fun saved(): String?

    /** Puts back what a project had, or null when it had none */
    fun restore(text: String?)
}

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
 * @param definitions The open A2L, saved with the project and opened with it; null leaves definitions alone
 * @param attachments Other things saved with a project, each as a file of its own
 * @param notify Shows a short message, like "Saved"
 * @param readInBackground Reads images on a background thread; false for tests and previews
 */
@Stable
class ProjectHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val repository: ProjectRepository = ProjectRepository(platformProjectStore()),
    private val definitions: DefinitionHandler? = null,
    private val attachments: List<ProjectAttachment> = emptyList(),
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

    /**
     * Whether the open image has been edited (or undone) since it was opened or saved, or a project has a definition
     * open that isn't the one saved with it
     */
    val hasUnsavedChanges: Boolean
        get() = documents.document != null && (documents.version != savedAt || definitionUnsaved)

    /** The additions' text as last opened or saved with the project, to tell when they've changed */
    private var savedAdditions by mutableStateOf<String?>(null)

    private val definitionUnsaved: Boolean
        get() = baseUnsaved || additionsUnsaved || attachmentsUnsaved

    /** Each attachment's text as last opened or saved with the project */
    private var savedAttachments by mutableStateOf<Map<String, String?>>(emptyMap())

    private val attachmentsUnsaved: Boolean
        get() = current != null && attachments.any { it.saved() != savedAttachments[it.fileName] }

    private val baseUnsaved: Boolean
        get() {
            val open = definitions?.definition?.takeIf { it.hasFile } ?: return false
            val saved = current?.manifest?.definition ?: return current != null
            return saved.fileName != open.name || saved.size != open.source.size.toLong()
        }

    private val additionsUnsaved: Boolean
        get() = current != null && definitions != null && definitions.definition?.additionsText() != savedAdditions

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
            savedAdditions = null
            savedAttachments = emptyMap()
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
            val definition = if (definitions != null && manifest.definition != null) repository.definition(manifest.id) else null
            val additions = if (definitions != null && manifest.additions != null) (repository.additions(manifest.id) as? TkResult.Success)?.data else null
            busy = false
            if (source !is TkResult.Success) return@launch failed("Open", (source as TkResult.Failure).error)
            if (patches !is TkResult.Success) return@launch failed("Open", (patches as TkResult.Failure).error)
            // A project opens with its own definition and maps made by hand, or none
            when (definition) {
                is TkResult.Success -> definition.data?.let { definitions?.open(it, manifest.definition!!.fileName, additions = additions ?: ByteArray(0)) } ?: definitions?.close()
                is TkResult.Failure -> failed("Open", definition.error)
                null -> if (additions != null) definitions?.openAdditions(additions) else definitions?.close()
            }
            savedAdditions = additions?.decodeToString()?.takeIf { it.isNotEmpty() }
            val texts = attachments.associate { attachment ->
                attachment.fileName to (repository.file(manifest.id, attachment.fileName) as? TkResult.Success)?.data?.decodeToString()
            }
            attachments.forEach { it.restore(texts[it.fileName]) }
            savedAttachments = texts
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
            savedAdditions = null
            savedAttachments = emptyMap()
            saveDefinition(manifest.id)?.let { return@launchSaving it }
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
            saveDefinition(open.manifest.id)?.let { return@launchSaving it }
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
            saveDefinition(open.manifest.id)?.let { return@launchSaving it }
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

    /**
     * Saves the open definition with project [id] when it isn't the one already there
     * @return Why it couldn't be, or null
     */
    private suspend fun saveDefinition(id: String): ProjectError? {
        val open = definitions?.definition
        if (open != null && baseUnsaved) {
            when (val result = repository.setDefinition(id, open.name, open.source)) {
                is TkResult.Success -> current = current?.copy(manifest = result.data)
                is TkResult.Failure -> return result.error
            }
        }
        for (attachment in attachments) {
            val text = attachment.saved()
            if (text == savedAttachments[attachment.fileName]) continue
            when (val result = repository.setFile(id, attachment.fileName, text?.encodeToByteArray())) {
                is TkResult.Success -> {
                    current = current?.copy(manifest = result.data)
                    savedAttachments = savedAttachments + (attachment.fileName to text)
                }
                is TkResult.Failure -> return result.error
            }
        }
        if (additionsUnsaved) {
            val text = open?.additionsText()
            when (val result = repository.setAdditions(id, text?.encodeToByteArray())) {
                is TkResult.Success -> {
                    current = current?.copy(manifest = result.data)
                    savedAdditions = text
                }
                is TkResult.Failure -> return result.error
            }
        }
        return null
    }

    /** Renames a version of the open project or changes its note */
    fun updateVersionInfo(versionId: String, name: String, note: String) {
        val open = current ?: return
        launchSaving("UpdateVersionInfo", markSaved = false) {
            when (val result = repository.updateVersionInfo(open.manifest.id, versionId, name, note)) {
                is TkResult.Success -> {
                    current = current?.copy(manifest = result.data)
                    "Saved"
                }
                is TkResult.Failure -> result.error
            }
        }
    }

    /**
     * Starts a new version of the open project from version [fromId] (or the original): a copy of its changes,
     * opened for editing, with [fromId] as what it's based on
     */
    fun branch(fromId: String, name: String, note: String, onOpened: () -> Unit = {}) {
        val manifest = current?.manifest ?: return
        launchSaving("Branch", markSaved = false) {
            val patches = when (val result = repository.patches(manifest.id, fromId)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return@launchSaving result.error
            }
            when (val added = repository.addVersion(manifest.id, name, patches, note, basedOn = fromId)) {
                is TkResult.Success -> {
                    openProject(added.data.first, added.data.second.id, onOpened)
                    "Started ${added.data.second.name}"
                }
                is TkResult.Failure -> added.error
            }
        }
    }

    /**
     * Version [versionId] of [manifest] as an image of its own, for comparing; null (with a message) when it can't
     * be read
     */
    suspend fun versionImage(manifest: ProjectManifest, versionId: String): MemoryImage? {
        val source = repository.source(manifest.id)
        val patches = repository.patches(manifest.id, versionId)
        if (source !is TkResult.Success) return null.also { failed("VersionImage", (source as TkResult.Failure).error) }
        if (patches !is TkResult.Success) return null.also { failed("VersionImage", (patches as TkResult.Failure).error) }
        val format = BinaryFormat.entries.firstOrNull { it.name == manifest.source.format } ?: BinaryFormat.detect(source.data)
        val image = when (val read = BinaryImport.read(source.data, manifest.source.fileName, manifest.source.rawBaseAddress, format)) {
            is TkResult.Success -> read.data.value
            is TkResult.Failure -> return null.also { notify(read.error.message) }
        }
        image.apply(patches.data.filter { image.contains(it.address, it.after.size) })
        image.clearHistory()
        return image
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
