package org.mr0btain.tunerstoolkit.ui.finder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Finding the demo's maps with and without its A2L, and adding one */
class FinderHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val a2l = File("../testdata/demo/tt-demo-1.a2l").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val definitions = DefinitionHandler(scope, documents, inBackground = false)
    private val finder = FinderHandler(scope, documents, definitions, inBackground = false)

    @Test
    fun withoutADefinitionTheMapsAreFoundAndOneIsAdded() {
        documents.open(hex, "tt-demo-1.hex", inBackground = false)
        finder.run()
        val results = assertNotNull(finder.results)
        assertTrue(results.count { it.kind == ObjectKind.MAP } >= 2, results.joinToString { it.label })
        assertEquals(1f, finder.progress)

        val map = results.first { it.kind == ObjectKind.MAP }
        assertEquals(map.values.toList(), finder.preview(map)!!.raw.toList())
        assertNull(finder.add(map, "Spark"))
        assertTrue(definitions.isAddition("Spark"))
        assertTrue(finder.results!!.none { it === map })
        assertTrue(definitions.decode(definitions.definition!!.set["Spark"]!!) is TkResult.Success)
        assertEquals("There's already something called Spark", finder.add(results.last(), "Spark"))
    }

    @Test
    fun withTheA2lOpenWhatItCoversIsSkipped() {
        definitions.open(a2l, "tt-demo-1.a2l")
        documents.open(hex, "tt-demo-1.hex", inBackground = false)
        finder.run()
        assertEquals(emptyList(), finder.results!!.map { it.label })
        finder.skipKnown = false
        finder.run()
        assertTrue(finder.results!!.isNotEmpty())

        // Another image drops the results
        documents.open(hex, "again.hex", inBackground = false)
        assertNull(finder.results)
    }
}
