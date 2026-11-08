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
import org.mr0btain.tunerstoolkit.core.a2l.A2lFolders
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
import org.mr0btain.tunerstoolkit.core.calibration.ObjectFolder
import org.mr0btain.tunerstoolkit.core.calibration.ObjectIndex
import org.mr0btain.tunerstoolkit.core.calibration.ObjectOverlap
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ttdef.TtDef
import org.mr0btain.tunerstoolkit.core.formats.xdf.Xdf
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.document.OpenDocument

/** Why a definition couldn't be opened */
class DefinitionError(override val message: String) : Error

/** The kinds of definition file */
enum class DefinitionFormat(val label: String, val extension: String) {
    A2L("ASAM A2L", "a2l"),
    XDF("TunerPro XDF", "xdf"),
    TTDEF("Tuners Toolkit definition", "ttdef");

    companion object {
        /** By content, not name */
        fun of(bytes: ByteArray): DefinitionFormat = when {
            Xdf.looksLike(bytes) -> XDF
            TtDef.looksLike(bytes) -> TTDEF
            else -> A2L
        }
    }
}

/** One way of foldering the map list: an A2L's functions or groups, an XDF's categories */
class Grouping(val label: String, val folders: List<ObjectFolder>)

/** What an A2L adds beyond its objects: the EPK, the report and subset export need the file itself */
class A2lSource(val file: A2lFile, val module: A2lModule)

/**
 * The definition that's open: a file (A2L, XDF or .ttdef) resolved into calibration objects, plus any maps made by
 * hand ([additions]); either can be there without the other
 * @param name The file's name; "Made here" when there's only additions
 * @param source The file as it was read, so it can be saved with a project; empty without one
 * @param imageStart Where an XDF's offsets start; XDFs are read again when an image starting elsewhere opens
 * @param diagnostics What reading and resolving the file found wrong
 */
class OpenDefinition(
    val name: String,
    val source: ByteArray,
    val format: DefinitionFormat?,
    val title: String,
    val base: CalibrationSet,
    val baseGroupings: List<Grouping>,
    val diagnostics: List<Diagnostic>,
    val additions: List<CalibrationObject> = emptyList(),
    val a2l: A2lSource? = null,
    val imageStart: Long = 0
) {
    val hasFile: Boolean get() = format != null

    /** The file's objects and the additions, as one set */
    val set: CalibrationSet = if (additions.isEmpty()) base else CalibrationSet(base.objects + additions, base.diagnostics)

    /** The map list's groupings, the additions in a folder of their own in each */
    val groupings: List<Grouping>
        get() {
            val made = additions.filter { it.kind != org.mr0btain.tunerstoolkit.core.calibration.ObjectKind.AXIS }.map { it.name }
            val folder = if (made.isEmpty()) emptyList() else listOf(ObjectFolder(MADE_HERE, objects = made))
            return baseGroupings.map { Grouping(it.label, it.folders + folder) }.ifEmpty { if (folder.isEmpty()) emptyList() else listOf(Grouping("Folders", folder)) }
        }

    /** Objects left out because they couldn't be resolved */
    val leftOut: Int get() = diagnostics.count { it.severity == Severity.ERROR && it.objectName != null }

    fun withAdditions(additions: List<CalibrationObject>) = OpenDefinition(name, source, format, title, base, baseGroupings, diagnostics, additions, a2l, imageStart)

    /** The additions as a .ttdef, for saving */
    fun additionsText(): String? = if (additions.isEmpty()) null else TtDef.write(CalibrationSet(additions), "Made in Tuners Toolkit")

    companion object {
        const val MADE_HERE = "Made here"
    }
}

/**
 * What the definition says about the open image: where each object is, which couldn't be read, and whether the EPK
 * matches
 */
class DefinitionOnImage(val document: OpenDocument, val build: IndexBuild, val epk: EpkResult, val overlaps: List<ObjectOverlap>)

/**
 * The open definition and maps made by hand, and what they say about the open image. Opening a different image indexes
 * it again
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
     * Reads [bytes] as a definition, whichever format it's in (A2L, XDF or .ttdef), keeping any maps made by hand
     * @param additions Maps made by hand to open with it, as a .ttdef (from a project)
     * @param onOpened Called once it's open
     */
    fun open(bytes: ByteArray, name: String, additions: ByteArray? = null, onOpened: (OpenDefinition) -> Unit = {}) {
        loading = true
        openError = null
        val kept = definition?.additions.orEmpty()
        val start = imageStart()
        run({ read(bytes, name, start) to additions?.let(::readAdditions) }) { (result, made) ->
            loading = false
            when (result) {
                is TkResult.Success -> {
                    definition = result.data.withAdditions(made ?: kept)
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

    /** Opens maps made by hand with no definition file under them (a project without one) */
    fun openAdditions(bytes: ByteArray) {
        val made = readAdditions(bytes)
        definition = empty().withAdditions(made)
        reindex()
    }

    fun close() {
        indexJob?.cancel()
        definition = null
        onImage = null
        indexing = false
    }

    /** Adds or replaces (by [replacing]'s name) maps made by hand: [objects] is a map and its axes */
    fun putAdditions(objects: List<CalibrationObject>, replacing: String? = null) {
        val current = definition ?: empty()
        val gone = replacing?.let { name -> setOf(name, "$name X axis", "$name Y axis") }.orEmpty() + objects.map { it.name }
        definition = current.withAdditions(current.additions.filter { it.name !in gone } + objects)
        reindex()
    }

    /** Removes a map made by hand, with the axes made for it */
    fun removeAddition(name: String) {
        val current = definition ?: return
        val gone = setOf(name, "$name X axis", "$name Y axis")
        val left = current.additions.filter { it.name !in gone }
        definition = if (left.isEmpty() && !current.hasFile) null else current.withAdditions(left)
        reindex()
    }

    /** Whether [name] is a map made by hand */
    fun isAddition(name: String): Boolean = definition?.additions?.any { it.name == name } == true

    /** Reads [obj] from the open image as it is now */
    fun decode(obj: CalibrationObject, image: MemoryImage? = documents.document?.image): TkResult<CalibrationValues, DecodeError>? {
        val set = definition?.set ?: return null
        return set.decode(image ?: return null, obj)
    }

    /** Builds the index of the open image again, for a new image or definition */
    fun reindex() {
        indexJob?.cancel()
        var definition = definition
        val document = documents.document
        if (definition == null || document == null) {
            onImage = null
            indexing = false
            return
        }
        // An XDF's offsets count from the image's first byte, so it's read again for an image starting elsewhere
        val start = document.image.ranges.firstOrNull()?.start ?: 0
        if (definition.format == DefinitionFormat.XDF && definition.imageStart != start) {
            val reread = read(definition.source, definition.name, start)
            if (reread is TkResult.Success) {
                definition = reread.data.withAdditions(definition.additions)
                this.definition = definition
            }
        }
        indexing = true
        val current = definition
        // The index reads the image while it's built; a snapshot keeps edits made meanwhile out of its way
        val image = if (inBackground) document.image.snapshot() else document.image
        indexJob = run({ index(current, document, image) }) { built ->
            onImage = built
            indexing = false
            AppLogger.i(
                "$TAG:Index",
                "Indexed ${document.name}: ${built.build.decoded} of ${current.set.objects.size} objects read, " +
                    "${built.overlaps.size} overlaps, EPK ${built.epk::class.simpleName}"
            )
        }
    }

    private fun imageStart(): Long = documents.document?.image?.ranges?.firstOrNull()?.start ?: 0

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

        fun empty() = OpenDefinition(OpenDefinition.MADE_HERE, ByteArray(0), null, OpenDefinition.MADE_HERE, CalibrationSet(emptyList()), emptyList(), emptyList())

        fun readAdditions(bytes: ByteArray): List<CalibrationObject> = when (val result = TtDef.read(bytes, "additions.ttdef")) {
            is TkResult.Success -> result.data.value.set.objects
            is TkResult.Failure -> emptyList<CalibrationObject>().also { AppLogger.w("$TAG:Additions", "Maps made by hand couldn't be read: ${result.error.message}") }
        }

        fun read(bytes: ByteArray, name: String, imageStart: Long): TkResult<OpenDefinition, DefinitionError> = when (DefinitionFormat.of(bytes)) {
            DefinitionFormat.XDF -> when (val result = Xdf.read(bytes, name, imageStart)) {
                is TkResult.Success -> {
                    val xdf = result.data.value
                    TkResult.Success(OpenDefinition(name, bytes, DefinitionFormat.XDF, xdf.title, xdf.set, listOf(Grouping("Categories", xdf.folders)), result.data.diagnostics, imageStart = imageStart))
                }
                is TkResult.Failure -> TkResult.Failure(DefinitionError(result.error.message))
            }
            DefinitionFormat.TTDEF -> when (val result = TtDef.read(bytes, name)) {
                is TkResult.Success -> {
                    val ttdef = result.data.value
                    val groupings = if (ttdef.folders.isEmpty()) emptyList() else listOf(Grouping("Folders", ttdef.folders))
                    TkResult.Success(OpenDefinition(name, bytes, DefinitionFormat.TTDEF, ttdef.title, ttdef.set, groupings, result.data.diagnostics))
                }
                is TkResult.Failure -> TkResult.Failure(DefinitionError(result.error.message))
            }
            DefinitionFormat.A2L -> readA2l(bytes, name)
        }

        fun readA2l(bytes: ByteArray, name: String): TkResult<OpenDefinition, DefinitionError> {
            val imported = when (val result = A2lImport.read(bytes, name)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return TkResult.Failure(DefinitionError(result.error.message))
            }
            val file = imported.value
            val module = file.modules.firstOrNull() ?: return TkResult.Failure(DefinitionError("The A2L has no MODULE"))
            val set = A2lResolver(module).resolve()
            val more = if (file.modules.size > 1) {
                listOf(Diagnostic(Severity.INFO, "The A2L has ${file.modules.size} modules; only the first, ${module.name}, is shown", module.element.location))
            } else {
                emptyList()
            }
            val groupings = listOf(Grouping("Functions", A2lFolders.byFunction(module, set)), Grouping("Groups", A2lFolders.byGroup(module, set)))
            return TkResult.Success(
                OpenDefinition(name, bytes, DefinitionFormat.A2L, module.longId.ifBlank { module.name }, set, groupings, imported.diagnostics + more + set.diagnostics, a2l = A2lSource(file, module))
            )
        }

        fun index(definition: OpenDefinition, document: OpenDocument, image: MemoryImage): DefinitionOnImage {
            val build = ObjectIndex.build(definition.set, image)
            val epk = definition.a2l?.let { EpkCheck.check(it.module, image) } ?: EpkResult.NotDescribed
            return DefinitionOnImage(document, build, epk, build.index.overlaps(limit = 200))
        }
    }
}
