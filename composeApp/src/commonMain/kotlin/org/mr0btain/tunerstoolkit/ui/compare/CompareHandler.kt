package org.mr0btain.tunerstoolkit.ui.compare

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
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.Compare
import org.mr0btain.tunerstoolkit.core.calibration.ImageComparison
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryImport
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.document.OpenDocument
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler

/** What the open image is compared with, and what to call it */
class OtherImage(val label: String, val image: MemoryImage, val document: OpenDocument)

/**
 * The image the open one is compared with (its original, a saved version, or another file) and how they differ,
 * worked out again in the background after every edit
 * @param inBackground Compares on a background thread; false does it now, for tests and previews
 */
@Stable
class CompareHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val definitions: DefinitionHandler,
    private val projects: ProjectHandler,
    private val inBackground: Boolean = true
) {
    var other by mutableStateOf<OtherImage?>(null)
        private set

    var comparison by mutableStateOf<ImageComparison?>(null)
        private set

    /** The document version [comparison] was made at */
    var comparedAt by mutableStateOf(Long.MIN_VALUE)
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    init {
        documents.onDocumentChanged { clear() }
    }

    fun compareWithOriginal() {
        val document = documents.document ?: return
        val original = documents.original() ?: return
        set(OtherImage("the original", original, document))
    }

    fun compareWithVersion(manifest: ProjectManifest, versionId: String) {
        val document = documents.document ?: return
        val name = manifest.version(versionId)?.name ?: "the original"
        busy = true
        scope.launch {
            val image = projects.versionImage(manifest, versionId)
            busy = false
            if (image == null) error = "$name couldn't be read" else set(OtherImage(name, image, document))
        }
    }

    fun compareWithFile(bytes: ByteArray, name: String, rawBaseAddress: Long = 0) {
        val document = documents.document ?: return
        when (val read = BinaryImport.read(bytes, name, rawBaseAddress)) {
            is TkResult.Success -> set(OtherImage(name, read.data.value, document))
            is TkResult.Failure -> error = "$name couldn't be read: ${read.error.message}"
        }
    }

    fun clear() {
        job?.cancel()
        other = null
        comparison = null
        error = null
        busy = false
    }

    private fun set(image: OtherImage) {
        other = image
        error = null
        AppLogger.i("$TAG:Compare", "Comparing ${image.document.name} with ${image.label}")
        refresh()
    }

    /** Compares again, after the open image changed */
    fun refresh() {
        val other = other ?: return
        val document = documents.document?.takeIf { it === other.document } ?: return clear()
        val index = definitions.onImage?.takeIf { it.document === document }?.build?.index
        val version = documents.version
        job?.cancel()
        busy = true
        val mine = if (inBackground) document.image.snapshot() else document.image
        val work = { Compare.images(mine, other.image, index) }
        if (!inBackground) {
            done(work(), version)
            return
        }
        job = scope.launch {
            val result = withContext(Dispatchers.Default) { work() }
            done(result, version)
        }
    }

    private fun done(result: ImageComparison, version: Long) {
        comparison = result
        comparedAt = version
        busy = false
    }

    /**
     * Copies [obj]'s bytes from the other image into the open one, as one undo step
     * @return How many bytes were copied
     */
    fun copyFromOther(obj: CalibrationObject): Int {
        val other = other ?: return 0
        val index = definitions.onImage?.build?.index ?: return 0
        val copied = documents.edit { image -> Compare.copyObject(obj, index, other.image, image) } ?: 0
        refresh()
        return copied
    }

    private companion object {
        const val TAG = "CompareHandler"
    }
}
