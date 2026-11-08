package org.mr0btain.tunerstoolkit.ui.checksum

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
import org.mr0btain.tunerstoolkit.core.checksum.CheckResult
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumCheck
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumModule
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumModules
import org.mr0btain.tunerstoolkit.core.checksum.Checksums
import org.mr0btain.tunerstoolkit.core.checksum.DefinedModule
import org.mr0btain.tunerstoolkit.core.checksum.DetectedModule
import org.mr0btain.tunerstoolkit.core.checksum.FixReport
import org.mr0btain.tunerstoolkit.core.checksum.ModuleStatus
import org.mr0btain.tunerstoolkit.core.checksum.TtSum
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectAttachment

/** The checks shown, and what each found, for one version of the image */
class ChecksumState(val version: Long, val checks: List<ChecksumCheck>, val results: List<CheckResult>, val problem: String?) {
    val wrong: Int get() = results.count { !it.ok }
}

/**
 * The open image's checksums: the module for its family (found by the modules' detectors, or picked), plus checks of
 * your own, worked out again in the background after every edit. Fixing is one undo step. The chosen module and your
 * own checks are saved with the project ([moduleAttachment], [ownAttachment])
 * @param inBackground Works on a background thread; false does it now, for tests and previews
 */
@Stable
class ChecksumHandler(
    private val scope: CoroutineScope,
    private val documents: DocumentHandler,
    private val inBackground: Boolean = true
) {
    /** Modules that come with the app and ones loaded from files */
    var modules by mutableStateOf(ChecksumModules.builtIn)
        private set

    /** Modules that recognise the open image, surest first */
    var detected by mutableStateOf<List<DetectedModule>>(emptyList())
        private set

    /** The module picked by hand, or null to use the one detected */
    var chosen by mutableStateOf<ChecksumModule?>(null)
        private set

    /** Checks of your own, alongside the module's */
    var own by mutableStateOf<DefinedModule?>(null)
        private set

    var state by mutableStateOf<ChecksumState?>(null)
        private set

    var working by mutableStateOf(false)
        private set

    private var job: Job? = null

    /** The module in use: the one picked, or the surest one detected */
    val active: ChecksumModule? get() = chosen ?: detected.firstOrNull()?.module

    init {
        documents.onDocumentChanged { detect() }
        // An image may already be open
        detect()
    }

    private fun detect() {
        detected = documents.document?.let { ChecksumModules.detect(it.image, modules) }.orEmpty()
        refresh()
    }

    fun choose(module: ChecksumModule?) {
        chosen = module
        refresh()
    }

    /**
     * Reads a .ttsum and uses it
     * @return What happened, in a line
     */
    fun load(bytes: ByteArray, name: String): String {
        val imported = when (val result = TtSum.read(bytes, name)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> return "$name isn't a checksum module: ${result.error.message}"
        }
        val module = imported.value
        modules = modules.filter { it.id != module.id } + module
        documents.document?.let { detected = ChecksumModules.detect(it.image, modules) }
        choose(module)
        AppLogger.i("$TAG:Load", "Loaded checksum module ${module.id} from $name, ${module.checkList.size} checks")
        return "Using ${module.name}" + if (imported.diagnostics.isNotEmpty()) ", ${imported.diagnostics.size} parts left out" else ""
    }

    fun addOwn(check: ChecksumCheck) {
        val current = own ?: DefinedModule("own", "Your checks", "", "Checks added in Tuners Toolkit", ModuleStatus.USER, "", emptyList(), emptyList())
        own = current.copy(checkList = current.checkList.filter { it.name != check.name } + check)
        refresh()
    }

    fun removeOwn(name: String) {
        val current = own ?: return
        own = current.copy(checkList = current.checkList.filter { it.name != name }).takeIf { it.checkList.isNotEmpty() }
        refresh()
    }

    /** Whether [check] is one of your own */
    fun isOwn(check: ChecksumCheck): Boolean = own?.checkList?.any { it.name == check.name } == true

    /** Works the checks out again for the image as it is now */
    fun refresh() {
        job?.cancel()
        val document = documents.document
        if (document == null) {
            state = null
            working = false
            return
        }
        val module = active
        val ownChecks = own?.checkList.orEmpty()
        val version = documents.version
        working = true
        val image = if (inBackground) document.image.snapshot() else document.image
        val work = { work(module, ownChecks, image, version) }
        if (!inBackground) {
            done(work())
            return
        }
        job = scope.launch { done(withContext(Dispatchers.Default) { work() }) }
    }

    private fun done(result: ChecksumState) {
        state = result
        working = false
    }

    /** Fixes every wrong checksum, as one undo step */
    fun fixAll(): FixReport? {
        val checks = state?.checks ?: return null
        val report = documents.edit { Checksums.fix(it, checks) } ?: return null
        AppLogger.i("$TAG:Fix", "Fixed ${report.fixed} checksums, ${report.results.count { !it.ok }} still wrong")
        refresh()
        return report
    }

    /** The chosen module, saved with the project when it isn't the one detected */
    val moduleAttachment = object : ProjectAttachment {
        override val fileName = "checksum-module.ttsum"
        override fun saved(): String? = (chosen as? DefinedModule)?.let(TtSum::write)
        override fun restore(text: String?) {
            chosen = text?.let { (TtSum.read(it.encodeToByteArray(), fileName) as? TkResult.Success)?.data?.value }
                ?.let { module -> modules.firstOrNull { it.id == module.id && it == module } ?: module.also { modules = modules.filter { m -> m.id != it.id } + it } }
            refresh()
        }
    }

    /** Your own checks, saved with the project */
    val ownAttachment = object : ProjectAttachment {
        override val fileName = "checksums-own.ttsum"
        override fun saved(): String? = own?.let(TtSum::write)
        override fun restore(text: String?) {
            own = text?.let { (TtSum.read(it.encodeToByteArray(), fileName) as? TkResult.Success)?.data?.value }
            refresh()
        }
    }

    private companion object {
        const val TAG = "ChecksumHandler"

        fun work(module: ChecksumModule?, ownChecks: List<ChecksumCheck>, image: MemoryImage, version: Long): ChecksumState {
            val moduleChecks = when (val result = module?.checks(image)) {
                is TkResult.Success -> result.data
                is TkResult.Failure -> return ChecksumState(version, ownChecks, Checksums.verify(image, ownChecks), result.error.message)
                null -> emptyList()
            }
            val checks = moduleChecks + ownChecks
            return ChecksumState(version, checks, Checksums.verify(image, checks), null)
        }
    }
}
