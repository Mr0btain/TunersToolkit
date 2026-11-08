package org.mr0btain.tunerstoolkit.core.formats

import org.mr0btain.tunerstoolkit.core.a2l.A2lFolders
import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.formats.ttdef.TtDef
import org.mr0btain.tunerstoolkit.core.formats.ttdef.TtDefinition
import org.mr0btain.tunerstoolkit.core.formats.xdf.Xdf
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class TtDefTest {
    private fun read(text: String): Imported<TtDefinition> = when (val result = TtDef.read(text.encodeToByteArray(), "test.ttdef")) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

    private fun sameValues(a: CalibrationSet, b: CalibrationSet, image: MemoryImage) {
        assertEquals(a.objects.map { it.name }, b.objects.map { it.name })
        for (obj in a.objects) {
            val x = a.decode(image, obj).getOrNull() ?: fail("${obj.name} can't be read")
            val y = b.decode(image, b[obj.name]!!).getOrNull() ?: fail("${obj.name} can't be read back")
            assertEquals(x.physical.toList(), y.physical.toList(), obj.name)
            assertEquals(x.axes.map { it.physical.toList() }, y.axes.map { it.physical.toList() }, obj.name)
            assertEquals(x.text, y.text)
            assertEquals(obj.unit, b[obj.name]!!.unit)
            assertEquals(obj.lowerLimit, b[obj.name]!!.lowerLimit)
        }
    }

    @Test
    fun theDemoDefinitionRoundTrips() {
        val demo = DemoEcu.build()
        val module = readA2l(demo.a2l).value.modules.single()
        val set = A2lResolver(module).resolve()
        val folders = A2lFolders.byFunction(module, set)
        val text = TtDef.write(set, "TT-DEMO-1", "From the A2L", folders)
        assertTrue(TtDef.looksLike(text.encodeToByteArray()))
        val back = read(text)
        assertEquals("TT-DEMO-1", back.value.title)
        assertEquals(folders.map { it.name to it.objects }, back.value.folders.map { it.name to it.objects })
        sameValues(set, back.value.set, demo.image)
        // Writing what was read gives the same file
        assertEquals(text, TtDef.write(back.value.set, "TT-DEMO-1", "From the A2L", back.value.folders))
    }

    @Test
    fun formulasKeepWhatTheCaretMeans() {
        val xdf = """<XDFFORMAT><XDFCONSTANT><title>P</title><EMBEDDEDDATA mmedaddress="0x0" mmedelementsizebits="8"/><MATH equation="2^X/(X+1)"><VAR id="X"/></MATH></XDFCONSTANT></XDFFORMAT>"""
        val set = (Xdf.read(xdf.encodeToByteArray()) as TkResult.Success).data.value.set
        val back = read(TtDef.write(set, "P")).value.set
        val image = MemoryImage.of(0L to byteArrayOf(3))
        assertEquals(2.0, back.decode(image, back["P"]!!).getOrNull()!!.physical.single(), "2^3 / 4")
        assertIs<Conversion.Formula>(back["P"]!!.conversion)
    }

    @Test
    fun brokenObjectsAreLeftOutAndNamed() {
        val text = """
            {"format": "ttdef", "formatVersion": 1, "title": "T", "objects": [
              {"name": "Good", "kind": "VALUE", "address": "0x10", "storage": {"byteOrder": "BIG_ENDIAN", "fields": [{"field": "values", "type": "UWORD"}]}},
              {"name": "Bad", "kind": "SPHERE", "address": "0x10", "storage": {"byteOrder": "BIG_ENDIAN", "fields": []}},
              {"name": "Worse", "kind": "VALUE", "address": "0x10", "storage": {"byteOrder": "BIG_ENDIAN", "fields": []}, "conversion": {"type": "formula", "formula": "X +"}}
            ]}
        """.trimIndent()
        val imported = read(text)
        assertEquals(listOf("Good"), imported.value.set.objects.map { it.name })
        assertEquals(listOf("Bad", "Worse"), imported.diagnostics.map { it.objectName })
        assertEquals(TkResult.Failure(ImportError.UNKNOWN_FORMAT), TtDef.read("not json".encodeToByteArray()))
        assertEquals(TkResult.Failure(ImportError.UNKNOWN_FORMAT), TtDef.read("""{"format": "other", "formatVersion": 1, "title": ""}""".encodeToByteArray()))
    }
}
