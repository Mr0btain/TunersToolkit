package org.mr0btain.tunerstoolkit.ui.finder

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.finder.FinderOptions
import org.mr0btain.tunerstoolkit.core.finder.FoundMap
import org.mr0btain.tunerstoolkit.core.finder.MapFinder
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler

/**
 * Looking for maps in the open image without a definition, in the background with progress, and turning a suggestion
 * into a map made by hand
 * @param inBackground Searches on a background thread; false does it now, for tests and previews
 */
@Stable
class FinderHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val definitions: DefinitionHandler,
    private val inBackground: Boolean = true
) {
    var options by mutableStateOf(FinderOptions())

    /** Leave out memory the open definition already covers */
    var skipKnown by mutableStateOf(true)

    var results by mutableStateOf<List<FoundMap>?>(null)
        private set

    var running by mutableStateOf(false)
        private set

    var progress by mutableFloatStateOf(0f)
        private set

    /** The suggestion being looked at */
    var selected by mutableStateOf<FoundMap?>(null)

    private var job: Job? = null
    private var stop = false

    init {
        documents.onDocumentChanged { cancel(); results = null; selected = null }
    }

    fun run() {
        val document = documents.document ?: return
        cancel()
        val skip = if (skipKnown) definitions.onImage?.takeIf { it.document === document }?.build?.index?.coverage.orEmpty() else emptyList()
        val chosen = options.copy(skip = skip)
        val image = if (inBackground) document.image.snapshot() else document.image
        stop = false
        running = true
        progress = 0f
        selected = null
        val work = { MapFinder.find(image, chosen, progress = { progress = it.toFloat() }, cancelled = { stop }) }
        if (!inBackground) {
            done(work())
            return
        }
        job = scope.launch { done(withContext(Dispatchers.Default) { work() }) }
    }

    private fun done(found: List<FoundMap>) {
        results = found
        running = false
        AppLogger.i("$TAG:Find", "Found ${found.size} possible maps${if (stop) " before stopping" else ""}")
    }

    fun cancel() {
        stop = true
        if (running) running = false
    }

    /** A suggestion's values (raw, as it would be added), for the preview */
    fun preview(found: FoundMap): CalibrationValues? {
        val image = documents.document?.image ?: return null
        val objects = found.toManualMap("Preview").objects()
        return CalibrationCodec.decode(image, objects.first(), CalibrationSet(objects)).getOrNull()
    }

    /**
     * Adds a suggestion as a map made by hand, and takes it off the list
     * @return Why it couldn't be, or null
     */
    fun add(found: FoundMap, name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "It needs a name"
        if (definitions.definition?.set?.get(trimmed) != null) return "There's already something called $trimmed"
        definitions.putAdditions(found.toManualMap(trimmed).objects())
        results = results?.filter { it !== found }
        if (selected === found) selected = null
        return null
    }

    private companion object {
        const val TAG = "FinderHandler"
    }
}
