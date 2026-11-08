package org.mr0btain.tunerstoolkit.ui.ident

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
import org.mr0btain.tunerstoolkit.core.ident.EcuIdent
import org.mr0btain.tunerstoolkit.core.ident.IdentFinding
import org.mr0btain.tunerstoolkit.core.project.ProjectInfo
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler

/**
 * What the open image says it is (hardware and software numbers, ECU family, calibration ID), read once when it
 * opens, in the background
 * @param inBackground Reads on a background thread; false does it now, for tests and previews
 */
@Stable
class IdentHandler(private val scope: CoroutineScope, private val documents: DocumentHandler, private val inBackground: Boolean = true) {
    var findings by mutableStateOf<List<IdentFinding>>(emptyList())
        private set

    var scanning by mutableStateOf(false)
        private set

    private var job: Job? = null

    init {
        documents.onDocumentChanged { scan() }
        scan()
    }

    private fun scan() {
        job?.cancel()
        val document = documents.document
        if (document == null) {
            findings = emptyList()
            return
        }
        // Read as it was opened: identification doesn't change with edits to maps
        val image = document.image
        scanning = true
        val work = { EcuIdent.scan(image) }
        if (!inBackground) {
            done(work(), document.name)
            return
        }
        job = scope.launch { done(withContext(Dispatchers.Default) { work() }, document.name) }
    }

    private fun done(found: List<IdentFinding>, name: String) {
        findings = found
        scanning = false
        AppLogger.i("$TAG:Scan", "$name: ${found.size} identification strings")
    }

    /** [current] with the ECU and software number filled in from what was found, where they're empty */
    fun suggest(current: ProjectInfo = ProjectInfo()): ProjectInfo = EcuIdent.suggest(findings, current)

    private companion object {
        const val TAG = "IdentHandler"
    }
}
