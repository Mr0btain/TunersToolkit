package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class A2lWriterTest {
    @Test
    fun theDemoA2lRoundTrips() {
        val written = assertRoundTrip(DemoEcu.build().a2l)
        // And still makes the same objects
        val again = readA2l(written)
        assertEquals(0, again.diagnostics.count { it.severity == Severity.ERROR })
        assertEquals(DemoEcu.build().placed.keys, A2lResolver(again.value.modules.single()).resolve().objects.map { it.name }.toSet())
    }

    @Test
    fun stringsKeepTheirEscapes() {
        // In the file: "Quote \" backslash \\ tab \t newline \n and ""doubled"""
        val name = "\"Quote \\\" backslash \\\\ tab \\t newline \\n and \"\"doubled\"\"\""
        val written = assertRoundTrip("/begin PROJECT P $name\n/end PROJECT")
        assertTrue("\\\"" in written)
        assertEquals("\"a\\\"b\\\\c\"", A2lWriter.quote("a\"b\\c"))
    }

    @Test
    fun unknownKeywordsRawBlocksAndNestingAreKept() {
        assertRoundTrip(
            """
            ASAP2_VERSION 1 71
            /begin PROJECT P ""
              /begin MODULE M ""
                /begin A2ML
                  block "IF_DATA" taggedunion { "XCP" struct { uint; }; };
                /end A2ML
                /begin IF_DATA XCP /begin SEGMENT 0 2 /end SEGMENT 0x10 "text" /end IF_DATA
                /begin IF_DATA EMPTY/end IF_DATA
                VENDOR_KEYWORD 1 2 "three"
                /begin VENDOR_BLOCK name 0x10
                  INNER_THING 4
                /end VENDOR_BLOCK
                /begin CHARACTERISTIC C "" VALUE 0x1000 L 0 NO_COMPU_METHOD -1.5e3 2.5E-2
                  /begin IF_DATA ETK KP_BLOB 0x1 /end IF_DATA
                /end CHARACTERISTIC
              /end MODULE
            /end PROJECT
            """.trimIndent()
        )
    }

    @Test
    fun aSubsetKeepsWhatItsObjectsNeed() {
        val demo = DemoEcu.build()
        val file = readA2l(demo.a2l).value
        val module = file.modules.single()
        val text = A2lWriter.subset(file, module, setOf("BoostTarget", "LaunchControl"))
        val subset = readA2l(text)
        assertEquals(0, subset.diagnostics.count { it.severity == Severity.ERROR }, subset.diagnostics.toString())
        val set = A2lResolver(subset.value.modules.single()).resolve()
        assertEquals(setOf("BoostTarget", "LaunchControl", "RpmAxis16", "LoadAxis12"), set.objects.map { it.name }.toSet())
        assertEquals(emptyList(), set.diagnostics.filter { it.severity != Severity.INFO })
        val subsetModule = subset.value.modules.single()
        assertTrue(subsetModule.compuVTabs.any { it.name == "onoff" }, "the TAB_VERB's table")
        assertTrue(subsetModule.measurements.isEmpty())
        assertTrue(subsetModule.functions.isEmpty())
        assertEquals(module.modPar?.epk, subsetModule.modPar?.epk)
        assertFalse("IgnitionBase" in text)
        // Reads the same values from the image as the full A2L
        val full = A2lResolver(module).resolve()
        val a = (full.decode(demo.image, full["BoostTarget"]!!) as org.mr0btain.tunerstoolkit.core.error.TkResult.Success).data
        val b = (set.decode(demo.image, set["BoostTarget"]!!) as org.mr0btain.tunerstoolkit.core.error.TkResult.Success).data
        assertEquals(a.physical.toList(), b.physical.toList())
    }
}

private fun org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet.decode(
    image: org.mr0btain.tunerstoolkit.core.image.MemoryImage,
    obj: org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
) = org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec.decode(image, obj, this)
