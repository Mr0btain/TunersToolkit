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
import org.mr0btain.tunerstoolkit.core.checksum.Detector
import org.mr0btain.tunerstoolkit.core.checksum.ModuleLibrary
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
    private val inBackground: Boolean = true,
    /** Where modules of your own are kept between runs; null keeps them only until the app closes */
    private val library: ModuleLibrary? = null
) {
    /** Modules that come with the app and ones loaded from files */
    var modules by mutableStateOf(ChecksumModules.builtIn)
        private set

    /** Ids of the modules in [modules] that are your own, kept in the library so every image can use them */
    var kept by mutableStateOf<Set<String>>(emptySet())
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
        if (library != null) scope.launch {
            val stored = library.loadAll().map(::untrusted).filter { stored -> ChecksumModules.builtIn.none { it.id == stored.id } }
            modules = modules.filter { module -> stored.none { it.id == module.id } } + stored
            kept = kept + stored.map { it.id }
            AppLogger.i("$TAG:Init", "${stored.size} checksum modules of your own")
            detect()
        }
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
        val module = untrusted(imported.value)
        keep(module)
        choose(module)
        AppLogger.i("$TAG:Load", "Loaded checksum module ${module.id} from $name, ${module.checkList.size} checks")
        return "Using ${module.name}" + if (imported.diagnostics.isNotEmpty()) ", ${imported.diagnostics.size} parts left out" else ""
    }

    /**
     * Makes a module of your own from your checks, kept for every image: it's used by itself on images that match
     * [detectors] (none means it's only used when picked). Your checks move into it, so they aren't counted twice
     * @return The module, or null with no checks of your own
     */
    fun makeModule(name: String, family: String, description: String, detectors: List<Detector>): DefinedModule? {
        val checks = own?.checkList?.takeIf { it.isNotEmpty() } ?: return null
        val id = ModuleLibrary.idFor(name).let { if (ChecksumModules.builtIn.any { builtIn -> builtIn.id == it }) "$it-own" else it }
        // Like any module from the library, it isn't taken as verified when it's read back, so it isn't now either
        val module = untrusted(DefinedModule(id, name.trim(), family.trim(), description.trim(), ModuleStatus.USER, "Made in Tuners Toolkit", detectors, checks))
        keep(module)
        own = null
        choose(module)
        AppLogger.i("$TAG:Make", "Made checksum module $id, ${checks.size} checks, ${detectors.size} detectors")
        return module
    }

    /** Takes a module of your own out of the library; one in use stops being used */
    fun forget(id: String) {
        if (id !in kept) return
        modules = modules.filter { it.id != id }
        kept = kept - id
        if (chosen?.id == id) chosen = null
        library?.let { scope.launch { it.delete(id) } }
        detect()
    }

    /** Adds [module] to the modules and the library, in place of one with its id */
    private fun keep(module: DefinedModule) {
        modules = modules.filter { it.id != module.id } + module
        kept = kept + module.id
        library?.let { scope.launch { it.save(module) } }
        documents.document?.let { detected = ChecksumModules.detect(it.image, modules) }
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

    /**
     * Fixes every wrong checksum, as one undo step. With a module that isn't verified it does nothing unless
     * [confirmedUnverified] (the person was asked); every way of fixing goes through here, so none can skip that.
     * The checks are worked out again for the image as it is now, not taken from the last background run
     * @return What fixing did, or null when there was nothing to fix with or it wasn't confirmed
     */
    fun fixAll(confirmedUnverified: Boolean = false): FixReport? {
        if (needsConfirmation && !confirmedUnverified) return null
        val document = documents.document ?: return null
        val checks = work(active, own?.checkList.orEmpty(), document.image, documents.version).checks
        if (checks.isEmpty()) return null
        val report = documents.edit { Checksums.fix(it, checks) } ?: return null
        AppLogger.i("$TAG:Fix", "Fixed ${report.fixed} checksums, ${report.results.count { !it.ok }} still wrong")
        refresh()
        return report
    }

    /**
     * Whether fixing has to be confirmed first: unless every check comes from a verified module built into the app,
     * it does. A file's word for its own status isn't taken (see [untrusted]), and checks of your own ask too, since a
     * project they came with may be someone else's
     */
    val needsConfirmation: Boolean
        get() {
            val module = active
            val trusted = module == null || (module.status == ModuleStatus.VERIFIED && ChecksumModules.builtIn.any { it === module })
            return !trusted || own != null
        }

    /** The chosen module, saved with the project when it isn't the one detected */
    val moduleAttachment = object : ProjectAttachment {
        override val fileName = "checksum-module.ttsum"
        override fun saved(): String? = (chosen as? DefinedModule)?.let(TtSum::write)
        override fun restore(text: String?) {
            chosen = text?.let { (TtSum.read(it.encodeToByteArray(), fileName) as? TkResult.Success)?.data?.value }
                ?.let { module ->
                    // The same as a built in one: that one. Anything else is the project's own, and not trusted
                    ChecksumModules.builtIn.firstOrNull { it == module }
                        ?: untrusted(module).also { loaded -> modules = modules.filter { it.id != loaded.id } + loaded }
                }
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

        /**
         * A module from a file or a project: whatever its file says (verified, or your own), it's taken as not
         * verified, and it can't take a built in module's place by using its id
         */
        fun untrusted(module: DefinedModule): DefinedModule {
            val clash = ChecksumModules.builtIn.any { it.id == module.id }
            return module.copy(
                id = if (clash) "${module.id}-loaded" else module.id,
                name = if (clash) "${module.name} (loaded)" else module.name,
                // Whatever the file says
                status = ModuleStatus.UNVERIFIED
            )
        }

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
