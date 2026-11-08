package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The demo A2L reads cleanly and every object DemoEcu wrote is there */
class DemoA2lTest {
    private val bytes = File("../testdata/demo/tt-demo-1.a2l").readBytes()

    @Test
    fun readsTheDemoWithoutProblems() {
        val imported = assertIs<TkResult.Success<*>>(A2lImport.read(bytes, "tt-demo-1.a2l")).data as org.mr0btain.tunerstoolkit.core.formats.Imported<A2lFile>
        assertEquals(emptyList(), imported.diagnostics.map { it.toString() })
        val file = imported.value
        assertEquals("1.61", file.asap2Version)
        val module = file.modules.single()
        assertEquals("DEMO_ECU", module.name)
        assertEquals(ByteOrder.LITTLE_ENDIAN, module.modCommon?.byteOrder)
        assertEquals(listOf(0x00800100L), module.modPar?.addrEpk)
        assertEquals(2, module.modPar?.memorySegments?.size)

        val text = bytes.decodeToString()
        fun count(keyword: String) = Regex("/begin $keyword\\b").findAll(text).count()
        assertEquals(count("CHARACTERISTIC"), module.characteristics.size)
        assertEquals(count("AXIS_PTS"), module.axisPts.size)
        assertEquals(count("MEASUREMENT"), module.measurements.size)
        assertEquals(count("COMPU_METHOD"), module.compuMethods.size)
        assertEquals(count("RECORD_LAYOUT"), module.recordLayouts.size)
        assertEquals(count("FUNCTION"), module.functions.size)

        // Every name the objects use is defined
        module.characteristics.forEach { characteristic ->
            assertTrue(module.recordLayout(characteristic.recordLayout) != null, "${characteristic.name}'s layout")
            assertTrue(characteristic.conversion == "NO_COMPU_METHOD" || module.compuMethod(characteristic.conversion) != null, "${characteristic.name}'s conversion")
            characteristic.axes.forEach { axis -> axis.axisPtsRef?.let { assertTrue(module.axisPts(it) != null, "${characteristic.name}'s axis $it") } }
        }
        println(A2lImport.summary(imported))
    }
}
