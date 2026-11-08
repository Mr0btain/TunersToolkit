package org.mr0btain.tunerstoolkit.ui.definition

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.a2l.A2lFile
import org.mr0btain.tunerstoolkit.core.a2l.A2lImport
import org.mr0btain.tunerstoolkit.core.a2l.A2lModule
import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.EpkCheck
import org.mr0btain.tunerstoolkit.core.a2l.EpkResult
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.DecodeError
import org.mr0btain.tunerstoolkit.core.calibration.IndexBuild
import org.mr0btain.tunerstoolkit.core.calibration.ObjectOverlap
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.document.OpenDocument

/** Why a definition couldn't be opened */
class DefinitionError(override val message: String) : org.mr0btain.tunerstoolkit.core.error.Error

/**
 * An A2L that's open: what was read, resolved into calibration objects
 * @param source The file as it was read, so it can be saved with a project
 * @param diagnostics What reading and resolving it found wrong
 */
class OpenDefinition(
    val name: String,
    val source: ByteArray,
    val file: A2lFile,
    val module: A2lModule,
    val set: CalibrationSet,
    val diagnostics: List<Diagnostic>
) {
    /** Objects left out because they couldn't be resolved */
    val leftOut: Int get() = diagnostics.count { it.severity == Severity.ERROR && it.objectName != null }
}

/**
 * What the definition says about the open image: where each object is, which couldn't be read, and whether the EPK
 * matches
 */
class DefinitionOnImage(val document: OpenDocument, val build: IndexBuild, val epk: EpkResult, val overlaps: List<ObjectOverlap>)

/**
 * The open definition (an A2L), and what it says about the open image. Opening a different image indexes it again
 * @param inBackground Reads and indexes on a background thread; false does it now, for tests and previews
 */
@Stable
class DefinitionHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val inBackground: Boolean = true
) {
    var definition by mutableStateOf<OpenDefinition?>(null)
        private set

    /** The index of the open image, once it's built; null while there's no image or no definition */
    var onImage by mutableStateOf<DefinitionOnImage?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var indexing by mutableStateOf(false)
        private set

    var openError by mutableStateOf<String?>(null)
        private set

    private var indexJob: Job? = null

    init {
        documents.onDocumentChanged { reindex() }
    }

    /**
     * Reads [bytes] as an A2L and resolves its first module
     * @param onOpened Called once it's open
     */
    fun open(bytes: ByteArray, name: String, onOpened: (OpenDefinition) -> Unit = {}) {
        loading = true
        openError = null
        run({ read(bytes, name) }) { result ->
            loading = false
            when (result) {
                is TkResult.Success -> {
                    definition = result.data
                    AppLogger.i("$TAG:Open", "Opened $name: ${result.data.set.objects.size} objects, ${result.data.leftOut} left out")
                    reindex()
                    onOpened(result.data)
                }
                is TkResult.Failure -> {
                    openError = result.error.message
                    AppLogger.w("$TAG:Open", "Couldn't open $name: ${result.error.message}")
                }
            }
        }
    }

    fun close() {
        indexJob?.cancel()
        definition = null
        onImage = null
        indexing = false
    }

    /** Reads [obj] from the open image as it is now */
    fun decode(obj: CalibrationObject, image: MemoryImage? = documents.document?.image): TkResult<CalibrationValues, DecodeError>? {
        val set = definition?.set ?: return null
        return set.decode(image ?: return null, obj)
    }

    /** Builds the index of the open image again, for a new image or definition */
    fun reindex() {
        indexJob?.cancel()
        val definition = definition
        val document = documents.document
        if (definition == null || document == null) {
            onImage = null
            indexing = false
            return
        }
        indexing = true
        // The index reads the image while it's built; a snapshot keeps edits made meanwhile out of its way
        val image = if (inBackground) document.image.snapshot() else document.image
        indexJob = run({ index(definition, document, image) }) { built ->
            onImage = built
            indexing = false
            AppLogger.i(
                "$TAG:Index",
                "Indexed ${document.name}: ${built.build.decoded} of ${definition.set.objects.size} objects read, " +
                    "${built.overlaps.size} overlaps, EPK ${built.epk::class.simpleName}"
            )
        }
    }

    private fun <T> run(work: () -> T, done: (T) -> Unit): Job? {
        if (!inBackground) {
            done(work())
            return null
        }
        return scope.launch {
            val result = withContext(Dispatchers.Default) { work() }
            done(result)
        }
    }

    private companion object {
        const val TAG = "DefinitionHandler"

        fun read(bytes: ByteArray, name: String): TkResult<OpenDefinition, DefinitionError> {
            val imported = when (val result = A2lImport.read(bytes, name)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return TkResult.Failure(DefinitionError(result.error.message))
            }
            val file = imported.value
            val module = file.modules.firstOrNull() ?: return TkResult.Failure(DefinitionError("The A2L has no MODULE"))
            val resolver = A2lResolver(module)
            val set = resolver.resolve()
            val more = if (file.modules.size > 1) {
                listOf(Diagnostic(Severity.INFO, "The A2L has ${file.modules.size} modules; only the first, ${module.name}, is shown", module.element.location))
            } else {
                emptyList()
            }
            return TkResult.Success(OpenDefinition(name, bytes, file, module, set, imported.diagnostics + more + set.diagnostics))
        }

        fun index(definition: OpenDefinition, document: OpenDocument, image: MemoryImage): DefinitionOnImage {
            val build = org.mr0btain.tunerstoolkit.core.calibration.ObjectIndex.build(definition.set, image)
            return DefinitionOnImage(document, build, EpkCheck.check(definition.module, image), build.index.overlaps(limit = 200))
        }
    }
}
