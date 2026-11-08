package org.mr0btain.tunerstoolkit.ui.compare

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Comparing the demo with its original and its saved versions, and copying maps across */
class CompareHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val a2l = File("../testdata/demo/tt-demo-1.a2l").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val definitions = DefinitionHandler(scope, documents, inBackground = false)
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), definitions, readInBackground = false)
    private val compare = CompareHandler(scope, documents, definitions, projects, inBackground = false)

    private fun setMap(name: String, value: Double) {
        val obj = definitions.definition!!.set[name]!!
        val values = (definitions.decode(obj) as TkResult.Success).data
        documents.edit { CalibrationCodec.writeAll(it, values, mapOf(0 to value)) }
    }

    private fun open() {
        definitions.open(a2l, "tt-demo-1.a2l")
        projects.openFile(hex, "tt-demo-1.hex")
    }

    @Test
    fun theOriginalShowsWhatChangedByMap() {
        open()
        compare.compareWithOriginal()
        assertEquals(0L, compare.comparison!!.changedBytes)

        setMap("RevLimit", 7000.0)
        compare.refresh()
        assertEquals(listOf("RevLimit"), compare.comparison!!.changedObjects.map { it.name })

        documents.write(0x10, byteArrayOf(0x42))
        compare.refresh()
        assertEquals(1, compare.comparison!!.outsideObjects.size)

        // Copying RevLimit from the original undoes its change, in one step
        assertEquals(2, compare.copyFromOther(definitions.definition!!.set["RevLimit"]!!))
        assertEquals(emptyList(), compare.comparison!!.changedObjects.map { it.name })
        documents.undo()
        compare.refresh()
        assertEquals(listOf("RevLimit"), compare.comparison!!.changedObjects.map { it.name })
    }

    @Test
    fun savedVersionsBranchesAndNotes() {
        open()
        setMap("MaxBoost", 2500.0)
        projects.saveAsProject("Bench", "Stage 1", "")
        val stage1 = projects.current!!.versionId

        // A new version from the original starts clean, based on it
        projects.branch(ProjectManifest.ORIGINAL, "Stage 2", "From scratch")
        val stage2 = assertNotNull(projects.current)
        assertEquals("Stage 2", stage2.versionName)
        assertEquals(ProjectManifest.ORIGINAL, stage2.version?.basedOn)
        assertEquals(0, documents.document!!.image.changedRanges().size)

        // Against Stage 1, MaxBoost differs
        compare.compareWithVersion(stage2.manifest, stage1)
        assertEquals("Stage 1", compare.other?.label)
        assertEquals(listOf("MaxBoost"), compare.comparison!!.changedObjects.map { it.name })

        projects.updateVersionInfo(stage1, "Stage 1 (road)", "Tested")
        val renamed = projects.current!!.manifest.version(stage1)!!
        assertEquals("Stage 1 (road)", renamed.name)
        assertEquals("Tested", renamed.note)

        // Opening another image drops the comparison
        projects.openFile(hex, "tt-demo-1.hex")
        assertNull(compare.comparison)
    }

    @Test
    fun anotherFile() {
        open()
        compare.compareWithFile(hex, "copy.hex")
        assertEquals(0L, compare.comparison!!.changedBytes)
        compare.compareWithFile(ByteArray(0), "empty.bin")
        assertTrue(compare.error!!.contains("empty.bin"))
    }
}
