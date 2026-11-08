package org.mr0btain.tunerstoolkit.core.formats

import org.mr0btain.tunerstoolkit.core.a2l.A2lFolders
import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.xdf.Xdf
import org.mr0btain.tunerstoolkit.core.formats.xdf.XdfDefinition
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlParser
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class XdfTest {
    private fun read(text: String, imageStart: Long = 0): Imported<XdfDefinition> = when (val result = Xdf.read(text.encodeToByteArray(), "test.xdf", imageStart)) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

    private fun CalibrationSet.values(image: MemoryImage, name: String): CalibrationValues = when (val result = decode(image, this[name] ?: fail("No $name in ${objects.map { it.name }}"))) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

    @Test
    fun theDemoGoesThroughXdfAndReadsTheSame() {
        val demo = DemoEcu.build()
        val module = readA2l(demo.a2l).value.modules.single()
        val set = A2lResolver(module).resolve()
        val written = Xdf.write(set, demo.image, "TT-DEMO-1", A2lFolders.byFunction(module, set))
        // Only the text can't go into an XDF
        assertEquals(listOf("SoftwareId"), written.skipped.map { it.objectName })

        val back = read(written.text, imageStart = demo.image.ranges.first().start)
        assertEquals(emptyList(), back.diagnostics.filter { it.severity != Severity.INFO }.map { it.toString() })
        assertEquals("TT-DEMO-1", back.value.title)
        for (obj in set.objects.filter { it.kind != ObjectKind.AXIS && it.name != "SoftwareId" }) {
            val a = set.values(demo.image, obj.name)
            val b = back.value.set.values(demo.image, obj.name)
            assertEquals(a.dimensions.toList(), b.dimensions.toList(), obj.name)
            a.physical.indices.forEach { i -> assertTrue(abs(a.physical[i] - b.physical[i]) < 1e-9, "${obj.name}[$i]: ${a.physical[i]} and ${b.physical[i]}") }
            a.axes.forEachIndexed { axis, points ->
                points.physical.indices.forEach { i -> assertTrue(abs(points.physical[i] - b.axes[axis].physical[i]) < 1e-9, "${obj.name} axis $axis[$i]") }
            }
        }
        // Categories are the A2L's functions
        assertEquals(listOf("Ignition", "Fueling", "Boost", "Driveability", "Limits", "Identification"), back.value.folders.map { it.name }.take(6))
        assertTrue("IgnitionBase" in back.value.folders.first { it.name == "Ignition" }.objects)
    }

    private val handWritten = """
        <?xml version="1.0" encoding="ISO-8859-1"?>
        <!DOCTYPE something>
        <XDFFORMAT version="1.60">
          <XDFHEADER>
            <deftitle>Hand made</deftitle>
            <BASEOFFSET offset="0x10" subtract="0" />
            <DEFAULTS datasizeinbits="8" lsbfirst="1" signed="0" float="0" />
            <CATEGORY index="0x0" name="Fuel" />
            <CATEGORY index="0x1" name="Spark &amp; timing" />
          </XDFHEADER>
          <XDFCONSTANT uniqueid="0x1">
            <title>Rev limit</title>
            <CATEGORYMEM index="0" category="2" />
            <EMBEDDEDDATA mmedaddress="0x0" mmedelementsizebits="16" />
            <units>rpm</units>
            <decimalpl>0</decimalpl>
            <MATH equation="Q*2"><VAR id="Q" /></MATH>
          </XDFCONSTANT>
          <XDFTABLE uniqueid="0x2">
            <title>Fuel map</title>
            <CATEGORYMEM index="0" category="1" />
            <XDFAXIS id="x"><indexcount>3</indexcount><embedinfo type="1" />
              <LABEL index="0" value="1000" /><LABEL index="1" value="2000" /><LABEL index="2" value="3000" />
            </XDFAXIS>
            <XDFAXIS id="y"><indexcount>2</indexcount><embedinfo type="3" linkobjid="0x4" /></XDFAXIS>
            <XDFAXIS id="z">
              <EMBEDDEDDATA mmedaddress="0x2" mmedelementsizebits="8" mmedrowcount="2" mmedcolcount="3" mmedtypeflags="0x04" />
              <min>0</min><max>100</max>
              <MATH equation="2^X"><VAR id="X" /></MATH>
            </XDFAXIS>
          </XDFTABLE>
          <XDFTABLE uniqueid="0x4">
            <title>Load points</title>
            <XDFAXIS id="x"><indexcount>2</indexcount><LABEL index="0" value="Lo" /><LABEL index="1" value="Hi" /></XDFAXIS>
            <XDFAXIS id="z"><EMBEDDEDDATA mmedaddress="0x8" mmedelementsizebits="8" mmedrowcount="1" mmedcolcount="2" /><MATH equation="X*10"><VAR id="X" /></MATH></XDFAXIS>
          </XDFTABLE>
          <XDFFLAG uniqueid="0x5">
            <title>Launch</title>
            <EMBEDDEDDATA mmedaddress="0xA" mmedelementsizebits="8" />
            <mask>0x04</mask>
          </XDFFLAG>
          <XDFCONSTANT uniqueid="0x6"><title>Rev limit</title><EMBEDDEDDATA mmedaddress="0xB" mmedelementsizebits="24" /></XDFCONSTANT>
          <XDFCONSTANT uniqueid="0x7"><title>Two inputs</title><EMBEDDEDDATA mmedaddress="0x0" /><MATH equation="X+Y"><VAR id="X" /><VAR id="Y" /></MATH></XDFCONSTANT>
          <XDFCONSTANT uniqueid="0x8"><title>Strided</title><EMBEDDEDDATA mmedaddress="0x0" mmedelementsizebits="8" mmedminorstridebits="16" /></XDFCONSTANT>
          <XDFCHECKSUM uniqueid="0x9"><title>Sum</title></XDFCHECKSUM>
        </XDFFORMAT>
    """.trimIndent()

    @Test
    fun everyKindOfItem() {
        val imported = read(handWritten, imageStart = 0x1000)
        val definition = imported.value
        val set = definition.set
        assertEquals("Hand made", definition.title)
        // Rev limit at 0x1000 + 0x10, LSB first by default
        val image = MemoryImage.of(0x1010L to byteArrayOf(0x20, 0x03, 1, 2, 3, 4, 5, 6, 2, 3, 0x04, 0, 0, 0, 0, 0, 0, 0, 0))
        val revLimit = set.values(image, "Rev limit")
        assertEquals(800.0 * 2, revLimit.physical.single(), "0x0320 = 800 (LSB first from the defaults), the VAR renamed to X")
        assertIs<Conversion.Linear>(set["Rev limit"]!!.conversion)
        assertEquals(ByteOrder.LITTLE_ENDIAN, set["Rev limit"]!!.storage.byteOrder)

        val fuel = set.values(image, "Fuel map")
        assertEquals(ValueOrder.Y_FIRST, set["Fuel map"]!!.storage.values!!.order, "column major")
        assertEquals(listOf(3, 2), fuel.dimensions.toList())
        assertEquals(listOf(1000.0, 2000.0, 3000.0), fuel.axes[0].physical.toList())
        assertEquals(listOf(20.0, 30.0), fuel.axes[1].physical.toList(), "linked to Load points' data")
        assertEquals(16.0, fuel[1, 1], "stored 4 at column 1 row 1; 2^4 as a power")
        assertTrue(set["Fuel map"]!!.axes[1].source is AxisSource.Shared)

        val load = set.values(image, "Load points")
        assertEquals("Hi", load.axes[0].text(1))

        val launch = set.values(image, "Launch")
        assertEquals("On", launch.textAt(0))

        assertEquals(listOf("Fuel", "Spark & timing"), definition.folders.map { it.name }.take(2))
        assertEquals(listOf("Rev limit"), definition.folders[1].objects)

        val errors = imported.diagnostics.filter { it.severity == Severity.ERROR }.map { it.objectName }
        assertEquals(listOf("Rev limit (2)", "Two inputs", "Strided"), errors, "24 bits, two variables, strides")
        assertTrue(imported.diagnostics.any { "XDFCHECKSUM" in it.message })
    }

    @Test
    fun notAnXdf() {
        assertTrue(Xdf.looksLike(handWritten.encodeToByteArray()))
        assertEquals(TkResult.Failure(ImportError.UNKNOWN_FORMAT), Xdf.read("<OTHER/>".encodeToByteArray()))
        assertEquals(TkResult.Failure(ImportError.EMPTY_FILE), Xdf.read(ByteArray(0)))
    }

    @Test
    fun equations() {
        assertEquals("X*0.75-48", Xdf.equation(Conversion.Linear(0.75, -48.0)))
        assertEquals("X", Xdf.equation(Conversion.Identical))
        assertEquals(null, Xdf.equation(Conversion.Table(listOf(0.0 to 1.0), true)))
    }

    @Test
    fun xmlIsReadLenientlyAndSafely() {
        val (root, problems) = XmlParser.parse("<a x='1' y=\"&lt;2&gt;\"><b>t&amp;t &#65;&#x42;</b><c/><![CDATA[<raw>]]><d>unclosed</a>")
        assertEquals("1", root!!.attribute("X"), "attributes in any case")
        assertEquals("<2>", root.attribute("y"))
        assertEquals("t&t AB", root.childText("b"))
        assertTrue("<raw>" in root.text)
        assertTrue(problems.isNotEmpty())
        val (deep, deepProblems) = XmlParser.parse("<a>".repeat(100_000) + "</a>".repeat(100_000))
        assertTrue(deep != null && deepProblems.any { "nested" in it.message })
    }
}
