package org.mr0btain.tunerstoolkit.ui.checksum

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumAlgorithm
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumCheck
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumModules
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumStore
import org.mr0btain.tunerstoolkit.core.checksum.TtSum
import org.mr0btain.tunerstoolkit.core.checksum.Detector
import org.mr0btain.tunerstoolkit.core.checksum.ModuleLibrary
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The demo's checksums found, broken by edits, fixed, and kept with a project */
class ChecksumHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val checksums = ChecksumHandler(scope, documents, inBackground = false)
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), attachments = listOf(checksums.moduleAttachment, checksums.ownAttachment), readInBackground = false)

    @Test
    fun theDemoModuleIsFoundAndFixes() {
        projects.openFile(hex, "tt-demo-1.hex")
        assertEquals("tt-demo-1", checksums.active?.id)
        assertEquals(3, checksums.state!!.checks.size)
        assertEquals(0, checksums.state!!.wrong)

        documents.write(0x804010, byteArrayOf(0x42))
        checksums.refresh()
        assertEquals(1, checksums.state!!.wrong)
        assertTrue(checksums.fixAll()!!.allOk)
        assertEquals(0, checksums.state!!.wrong)
        documents.undo()
        checksums.refresh()
        assertEquals(1, checksums.state!!.wrong, "fixing is its own undo step")
    }

    @Test
    fun yourOwnChecksAndALoadedModuleAreSavedWithTheProject() {
        projects.openFile(hex, "tt-demo-1.hex")
        projects.saveAsProject("Bench", "Stage 1", "")
        assertFalse(projects.hasUnsavedChanges)

        checksums.addOwn(ChecksumCheck("Mine", ChecksumAlgorithm.Sum(1, 16), listOf(AddressRange(0x800200, 0x800300)), ChecksumStore(0x800300, 2)))
        assertTrue(projects.hasUnsavedChanges)
        assertEquals(4, checksums.state!!.checks.size)
        val module = ChecksumModules.demo.copy(id = "loaded", name = "Loaded")
        assertEquals("Using Loaded", checksums.load(TtSum.write(module).encodeToByteArray(), "loaded.ttsum"))
        assertEquals("Loaded", checksums.active?.name)
        projects.saveAsVersion("Stage 2", "")
        assertFalse(projects.hasUnsavedChanges)

        // Open something else, then the project: both come back
        checksums.choose(null)
        checksums.removeOwn("Mine")
        projects.openFile(hex, "tt-demo-1.hex")
        assertNull(checksums.own)
        projects.openProject(projects.projects.single())
        assertEquals("Loaded", checksums.active?.name)
        assertEquals(listOf("Mine"), checksums.own!!.checkList.map { it.name })
        assertFalse(projects.hasUnsavedChanges)
        assertTrue(checksums.load("not a module".encodeToByteArray(), "x.ttsum").contains("isn't a checksum module"))
    }

    @Test
    fun filesCantVouchForThemselvesOrReplaceABuiltInModule() {
        projects.openFile(hex, "tt-demo-1.hex")
        // A file claiming to be the verified demo module
        val impostor = ChecksumModules.demo.copy(description = "Something else")
        checksums.load(TtSum.write(impostor).encodeToByteArray(), "impostor.ttsum")
        val loaded = checksums.active!!
        assertEquals("tt-demo-1-loaded", loaded.id)
        assertEquals(org.mr0btain.tunerstoolkit.core.checksum.ModuleStatus.UNVERIFIED, loaded.status)
        assertTrue(checksums.modules.any { it === ChecksumModules.demo }, "the built in one is still there")

        // Fixing with it needs confirming, whoever asks
        documents.write(0x804010, byteArrayOf(0x42))
        assertTrue(checksums.needsConfirmation)
        assertNull(checksums.fixAll())
        assertTrue(checksums.fixAll(confirmedUnverified = true)!!.allOk)

        // Nor can a file claim to be your own to skip asking
        val mine = ChecksumModules.demo.copy(id = "mine", status = org.mr0btain.tunerstoolkit.core.checksum.ModuleStatus.USER)
        checksums.load(TtSum.write(mine).encodeToByteArray(), "mine.ttsum")
        assertEquals(org.mr0btain.tunerstoolkit.core.checksum.ModuleStatus.UNVERIFIED, checksums.active!!.status)
        assertTrue(checksums.needsConfirmation)

        // The built in, verified module fixes without asking; adding checks of your own makes it ask
        checksums.choose(null)
        assertFalse(checksums.needsConfirmation)
        checksums.addOwn(ChecksumCheck("Mine", ChecksumAlgorithm.Sum(1, 16), listOf(AddressRange(0x800200, 0x800300)), ChecksumStore(0x800300, 2)))
        assertTrue(checksums.needsConfirmation)
    }

    @Test
    fun fixingUsesTheImageAsItIsNow() {
        projects.openFile(hex, "tt-demo-1.hex")
        // An edit the background check hasn't seen yet
        documents.write(0x804010, byteArrayOf(0x42))
        assertEquals(0, checksums.state!!.wrong)
        val report = checksums.fixAll()!!
        assertEquals(1, report.fixed, "the calibration CRC, which the stale results said was right")
        val image = documents.document!!.image
        assertTrue(org.mr0btain.tunerstoolkit.core.checksum.Checksums.verify(image, ChecksumModules.demo.checkList).all { it.ok })
    }

    @Test
    fun yourOwnChecksKeptAsAModuleAreUsedForEveryImageAfterARestart() {
        val store = MemoryProjectStore()
        val first = ChecksumHandler(scope, documents, inBackground = false, library = ModuleLibrary(store))
        projects.openFile(hex, "tt-demo-1.hex")
        first.addOwn(ChecksumCheck("Mine", ChecksumAlgorithm.Sum(1, 16), listOf(AddressRange(0x800200, 0x800300)), ChecksumStore(0x800300, 2)))
        val size = documents.document!!.image.size
        val module = first.makeModule("Bench ECU", "Example", "", listOf(Detector.Size(size)))!!
        assertNull(first.own, "the checks moved into the module")
        assertEquals(module.id, first.active?.id)
        assertTrue(module.id in first.kept)
        assertTrue(first.needsConfirmation, "a module of your own isn't verified")

        // Another run of the app: the module is there, and recognises an image of that size by itself
        val second = ChecksumHandler(scope, documents, inBackground = false, library = ModuleLibrary(store))
        assertTrue(second.modules.any { it.id == "bench-ecu" })
        assertTrue(second.detected.any { it.module.id == "bench-ecu" })
        assertEquals(listOf("Mine"), (second.modules.single { it.id == "bench-ecu" } as org.mr0btain.tunerstoolkit.core.checksum.DefinedModule).checkList.map { it.name })

        // A loaded .ttsum is kept too, and forgetting takes either out
        second.load(TtSum.write(ChecksumModules.demo.copy(id = "loaded", name = "Loaded")).encodeToByteArray(), "loaded.ttsum")
        second.forget("bench-ecu")
        assertTrue(second.modules.none { it.id == "bench-ecu" })
        val third = ChecksumHandler(scope, documents, inBackground = false, library = ModuleLibrary(store))
        assertEquals(setOf("loaded"), third.kept)
    }
}
