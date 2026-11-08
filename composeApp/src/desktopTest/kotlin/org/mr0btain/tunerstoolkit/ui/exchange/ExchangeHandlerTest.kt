package org.mr0btain.tunerstoolkit.ui.exchange

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.ManualAxis
import org.mr0btain.tunerstoolkit.core.calibration.ManualMap
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionFormat
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Definitions in every format, maps made by hand, and values in and out, with the demo files */
class ExchangeHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val a2l = File("../testdata/demo/tt-demo-1.a2l").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val definitions = DefinitionHandler(scope, documents, inBackground = false)
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), definitions, readInBackground = false)
    private val exchange = ExchangeHandler(documents, definitions)

    private fun open() {
        definitions.open(a2l, "tt-demo-1.a2l")
        projects.openFile(hex, "tt-demo-1.hex")
    }

    private fun <T> TkResult<T, ExchangeError>.ok(): T = when (this) {
        is TkResult.Success -> data
        is TkResult.Failure -> fail(error.message)
    }

    private fun physical(name: String, index: Int = 0): Double =
        (definitions.decode(definitions.definition!!.set[name]!!) as TkResult.Success).data.physical[index]

    @Test
    fun theDefinitionAsXdfAndTtdefOpensAgainAndReadsTheSame() {
        open()
        val objects = definitions.onImage!!.build.decoded
        for (format in listOf(DefinitionFormat.XDF, DefinitionFormat.TTDEF)) {
            val exported = exchange.exportDefinition(format).ok()
            definitions.open(exported.bytes, "demo.${format.extension}")
            val definition = definitions.definition!!
            assertEquals(format, definition.format)
            assertEquals(6800.0, physical("RevLimit"), "$format")
            assertTrue(definitions.onImage!!.build.decoded >= objects - 1, "$format reads (almost) everything: the XDF can't hold the text")
            definitions.open(a2l, "tt-demo-1.a2l")
        }
    }

    @Test
    fun valuesGoOutAndBackIn() {
        open()
        val set = definitions.definition!!.set
        val values = (definitions.decode(set["RevLimit"]!!) as TkResult.Success).data
        documents.edit { CalibrationCodec.writeAll(it, values, mapOf(0 to 7000.0)) }
        val changed = exchange.exportValues(ValuesFormat.DCM, ExportScope.CHANGED).ok()
        assertEquals(1, changed.count)

        documents.undo()
        assertEquals(6800.0, physical("RevLimit"))
        val all = exchange.exportValues(ValuesFormat.CDFX, ExportScope.ALL).ok()
        assertTrue(exchange.importValues(changed.bytes, "changed.dcm").startsWith("1 maps written"))
        assertEquals(7000.0, physical("RevLimit"))
        assertNotNull(exchange.lastImport)

        // The CDFX holds 6800 again
        exchange.importValues(all.bytes, "all.cdfx")
        assertEquals(6800.0, physical("RevLimit"))
        assertTrue(exchange.importValues("nonsense".encodeToByteArray(), "x.txt").contains("isn't a DCM"))
    }

    @Test
    fun aMapAsCsv() {
        open()
        val obj = definitions.definition!!.set["IgnitionBase"]!!
        val csv = exchange.exportMapCsv(obj).ok()
        val edited = csv.bytes.decodeToString().lines().mapIndexed { i, line -> if (i == 1) line.substringBefore(';') + ";30" + line.substringAfter(';').substringAfter(';').let { ";$it" } else line }.joinToString("\n")
        assertEquals("Read into IgnitionBase", exchange.importMapCsv(obj, edited.encodeToByteArray()))
        assertEquals(30.0, physical("IgnitionBase"), 0.2)
    }

    @Test
    fun mapsMadeByHandAreSavedWithTheProject() {
        projects.openFile(hex, "tt-demo-1.hex")
        assertNull(definitions.definition)
        val made = ManualMap(name = "Found", kind = ObjectKind.CURVE, address = 0x0080_4000, columns = 4, x = ManualAxis(ManualAxis.Source.EVEN, 0.0, 30.0))
        definitions.putAdditions(made.objects())
        assertTrue(definitions.isAddition("Found"))
        assertFalse(definitions.definition!!.hasFile)
        assertNotNull(definitions.onImage)

        projects.saveAsProject("Bench", "Stage 1", "")
        assertFalse(projects.hasUnsavedChanges)
        assertEquals("additions.ttdef", projects.projects.single().additions?.fileName)

        // Changing it is unsaved until saved
        definitions.putAdditions(made.copy(unit = "°").objects(), replacing = "Found")
        assertTrue(projects.hasUnsavedChanges)
        projects.saveAsVersion("Stage 2", "")
        assertFalse(projects.hasUnsavedChanges)

        definitions.close()
        projects.openProject(projects.projects.single())
        assertEquals("°", definitions.definition!!.set["Found"]!!.unit)
        assertFalse(projects.hasUnsavedChanges)

        // With an A2L opened later, the made map stays alongside
        definitions.open(a2l, "tt-demo-1.a2l")
        assertNotNull(definitions.definition!!.set["Found"])
        assertNotNull(definitions.definition!!.set["IgnitionBase"])
        assertTrue(definitions.definition!!.groupings.first().folders.any { it.name == "Made here" })
        definitions.removeAddition("Found")
        assertNull(definitions.definition!!.set["Found"])
    }
}
