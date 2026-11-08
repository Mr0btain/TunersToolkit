package org.mr0btain.tunerstoolkit.core.formats

import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.Compare
import org.mr0btain.tunerstoolkit.core.calibration.ObjectIndex
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.formats.values.CalibrationData
import org.mr0btain.tunerstoolkit.core.formats.values.Cdfx
import org.mr0btain.tunerstoolkit.core.formats.values.Csv
import org.mr0btain.tunerstoolkit.core.formats.values.DataRecord
import org.mr0btain.tunerstoolkit.core.formats.values.Dcm
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ValuesExchangeTest {
    private val demo = DemoEcu.build()
    private val set = A2lResolver(readA2l(demo.a2l).value.modules.single()).resolve()
    private val index = ObjectIndex.build(set, demo.image).index

    private fun values(image: MemoryImage, name: String): CalibrationValues = set.decode(image, set[name]!!).getOrNull() ?: fail("$name can't be read")

    /** The demo with a few maps changed, values and axis points */
    private fun tuned(): MemoryImage = demo.image.snapshot().also { image ->
        CalibrationCodec.writeAll(image, values(image, "IgnitionBase"), mapOf(0 to 20.0, 30 to 25.0))
        CalibrationCodec.writeAll(image, values(image, "RevLimit"), mapOf(0 to 7200.0))
        CalibrationCodec.writeAll(image, values(image, "LaunchControl"), mapOf(0 to 1.0))
        CalibrationCodec.writeAxis(image, values(image, "WarmupEnrichment"), 0, 1, -15.0)
        CalibrationCodec.writeAscii(image, values(image, "SoftwareId"), "TUNED")
        CalibrationCodec.writeAll(image, values(image, "RpmAxis16"), mapOf(15 to 6700.0))
    }

    private fun <T> ok(result: TkResult<Imported<T>, ImportError>): Imported<T> = when (result) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

    private fun assertBackToTheDemo(records: List<DataRecord>) {
        val image = tuned()
        assertTrue(Compare.images(image, demo.image, index).changedObjects.isNotEmpty())
        val report = CalibrationData.apply(records, set, image)
        assertEquals(emptyList(), report.problems.filter { it.severity == Severity.ERROR }.map { it.toString() })
        assertEquals(emptyList(), report.missing)
        assertEquals(emptyList(), Compare.images(image, demo.image, index).regions.map { "${it.range} ${it.objects}" })
        assertEquals(6, report.applied, report.summary)
        image.undo()
        assertTrue(Compare.images(image, demo.image, index).regions.isNotEmpty(), "applying is one undo step")
    }

    @Test
    fun dcmTakesEveryMapBack() {
        val (records, problems) = CalibrationData.records(set, demo.image)
        assertTrue(problems.isEmpty())
        val (text, skipped) = Dcm.write(records)
        assertEquals(emptyList(), skipped.map { it.toString() })
        assertTrue(Dcm.looksLike(text.encodeToByteArray()))
        assertTrue("GRUPPENKENNFELD BoostTarget 16 12" in text)
        assertTrue("FESTKENNLINIE GearTorqueLimit 6" in text)
        assertTrue("TEXT \"Off\"" in text)
        val read = ok(Dcm.read(text.encodeToByteArray(), "demo.dcm"))
        assertEquals(emptyList(), read.diagnostics.map { it.toString() })
        assertBackToTheDemo(read.value)
    }

    @Test
    fun cdfxTakesEveryMapBack() {
        val (records, _) = CalibrationData.records(set, demo.image)
        val (text, skipped) = Cdfx.write(records, "DEMO_ECU")
        assertEquals(emptyList(), skipped.map { it.toString() })
        assertTrue(Cdfx.looksLike(text.encodeToByteArray()))
        val read = ok(Cdfx.read(text.encodeToByteArray(), "demo.cdfx"))
        assertEquals(emptyList(), read.diagnostics.map { it.toString() })
        assertEquals(records.map { it.name }, read.value.map { it.name })
        assertBackToTheDemo(read.value)
    }

    @Test
    fun applyingSaysWhatDidntFit() {
        val image = demo.image.snapshot()
        val records = listOf(
            DataRecord("Nowhere", ObjectKind.VALUE, emptyList(), listOf(1.0)),
            DataRecord("InjectorTrim", ObjectKind.VAL_BLK, listOf(3), listOf(1.0, 1.0, 1.0)),
            DataRecord("RevLimit", ObjectKind.VALUE, emptyList(), listOf(99999.0)),
            DataRecord("LaunchControl", ObjectKind.VALUE, emptyList(), emptyList(), texts = listOf("On")),
            DataRecord("IgnitionBase", ObjectKind.MAP, listOf(12, 16), List(192) { 10.0 }),
            DataRecord(
                "BoostTarget", ObjectKind.MAP, listOf(16, 12), values(image, "BoostTarget").physical.toList(),
                axes = listOf(org.mr0btain.tunerstoolkit.core.formats.values.DataAxis(List(16) { it * 100.0 }), org.mr0btain.tunerstoolkit.core.formats.values.DataAxis(emptyList()))
            )
        )
        val report = CalibrationData.apply(records, set, image)
        assertEquals(listOf("Nowhere"), report.missing)
        assertEquals(1, report.clamped, "RevLimit set to its limit")
        assertEquals(9000.0, values(image, "RevLimit").physical.single())
        assertEquals("On", values(image, "LaunchControl").textAt(0))
        val problems = report.problems.associate { it.objectName to it.severity }
        assertEquals(Severity.ERROR, problems["InjectorTrim"], "3 values for 4")
        assertEquals(Severity.ERROR, problems["IgnitionBase"], "12 × 16 for 16 × 12")
        assertEquals(Severity.WARNING, problems["BoostTarget"], "a shared axis differs")
        assertTrue("shared" in report.problems.first { it.objectName == "BoostTarget" }.message)
        assertTrue(report.summary.contains("not in the definition"))
    }

    @Test
    fun handWrittenDcm() {
        val text = """
            * A comment
            KONSERVIERUNG_FORMAT 2.0
            FUNKTIONEN
              FKT Ignition "" "Spark"
            END
            FESTWERT RevLimit
              LANGNAME "Limit"
              EINHEIT_W "rpm"
              WERT 6500,5
            END
            FESTWERTEBLOCK Grid 3 @ 2
              WERT 1 2 3
              WERT 4 5 6
            END
            GRUPPENKENNLINIE Curve 4
            *SSTX    SpeedAxis
              ST/X 1 2
              ST/X 3 4
              WERT 10 20
              WERT 30 40
            END
            KENNFELD Map 2 2
              ST/X 100 200
              ST/Y 1
              WERT 11 21
              ST/Y 2
              WERT 12 22
            END
            WHATEVER thing
            TEXTSTRING Id
              TEXT "abc"
            END
        """.trimIndent()
        val read = ok(Dcm.read(text.encodeToByteArray(), "hand.dcm"))
        val records = read.value.associateBy { it.name }
        assertEquals(listOf(6500.5), records["RevLimit"]!!.values, "a comma decimal point")
        assertEquals("Limit", records["RevLimit"]!!.description)
        assertEquals(listOf(3, 2), records["Grid"]!!.dimensions)
        assertEquals("SpeedAxis", records["Curve"]!!.axes.single().shared)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), records["Curve"]!!.axes.single().values)
        assertEquals(listOf(11.0, 21.0, 12.0, 22.0), records["Map"]!!.values, "x first")
        assertEquals(listOf(1.0, 2.0), records["Map"]!!.axes[1].values)
        assertEquals("abc", records["Id"]!!.text)
        assertEquals(listOf("Unknown keyword WHATEVER, skipped"), read.diagnostics.map { it.message })
    }

    @Test
    fun handWrittenCdfx() {
        val text = """
            <MSRSW><SW-SYSTEMS><SW-SYSTEM><SW-INSTANCE-SPEC><SW-INSTANCE-TREE>
              <SW-INSTANCE><SHORT-NAME>Flat</SHORT-NAME><CATEGORY>MAP</CATEGORY>
                <SW-VALUE-CONT><SW-VALUES-PHYS><V>11</V><V>12</V><V>21</V><V>22</V><V>31</V><V>32</V></SW-VALUES-PHYS></SW-VALUE-CONT>
                <SW-AXIS-CONTS>
                  <SW-AXIS-CONT><CATEGORY>STD_AXIS</CATEGORY><SW-VALUES-PHYS><V>1</V><V>2</V><V>3</V></SW-VALUES-PHYS></SW-AXIS-CONT>
                  <SW-AXIS-CONT><CATEGORY>COM_AXIS</CATEGORY><SW-INSTANCE-REF>Ys</SW-INSTANCE-REF><SW-VALUES-PHYS><V>10</V><V>20</V></SW-VALUES-PHYS></SW-AXIS-CONT>
                </SW-AXIS-CONTS>
              </SW-INSTANCE>
              <SW-INSTANCE><SHORT-NAME>Mode</SHORT-NAME><CATEGORY>VALUE</CATEGORY><SW-VALUE-CONT><SW-VALUES-PHYS><VT>On</VT></SW-VALUES-PHYS></SW-VALUE-CONT></SW-INSTANCE>
              <SW-INSTANCE><SHORT-NAME>Odd</SHORT-NAME><CATEGORY>STRUCTURE</CATEGORY></SW-INSTANCE>
              <SW-INSTANCE><SHORT-NAME>Short</SHORT-NAME><CATEGORY>CURVE</CATEGORY>
                <SW-VALUE-CONT><SW-VALUES-PHYS><V>1</V></SW-VALUES-PHYS></SW-VALUE-CONT>
                <SW-AXIS-CONTS><SW-AXIS-CONT><SW-VALUES-PHYS><V>1</V><V>2</V></SW-VALUES-PHYS></SW-AXIS-CONT></SW-AXIS-CONTS>
              </SW-INSTANCE>
            </SW-INSTANCE-TREE></SW-INSTANCE-SPEC></SW-SYSTEM></SW-SYSTEMS></MSRSW>
        """.trimIndent()
        val read = ok(Cdfx.read(text.encodeToByteArray(), "hand.cdfx"))
        val records = read.value.associateBy { it.name }
        val flat = records["Flat"]!!
        assertEquals(listOf(3, 2), flat.dimensions, "from the axes")
        assertEquals(listOf(11.0, 21.0, 31.0, 12.0, 22.0, 32.0), flat.values, "listed x by x, stored x first")
        assertEquals("Ys", flat.axes[1].shared)
        assertEquals(listOf("On"), records["Mode"]!!.texts)
        assertEquals(setOf("Odd", "Short"), read.diagnostics.mapNotNull { it.objectName }.toSet())
    }

    @Test
    fun csvMapsGoBothWays() {
        val image = demo.image.snapshot()
        val ignition = values(image, "IgnitionBase")
        val text = Csv.writeMap(ignition)
        assertEquals(13, text.trim().lines().size)
        val record = (Csv.readMap(text, ignition) as TkResult.Success).data
        assertEquals(ignition.physical.toList(), record.values)
        assertEquals(ignition.axes[0].physical.toList(), record.axes[0].values)

        // Changed in a spreadsheet with comma decimals and tabs, then read back in
        val edited = text.replace(";", "\t").lines().mapIndexed { i, line -> if (i == 1) line.replaceFirst(Regex("\t[^\t]+"), "\t30,5") else line }.joinToString("\n")
        val back = (Csv.readMap(edited, ignition) as TkResult.Success).data
        assertEquals(30.5, back.values[0])
        CalibrationData.apply(listOf(back), set, image)
        assertEquals(30.375, values(image, "IgnitionBase")[0, 0], "the nearest stored value")

        val curve = values(image, "WarmupEnrichment")
        assertEquals(curve.physical.toList(), (Csv.readMap(Csv.writeMap(curve), curve) as TkResult.Success).data.values)
        val value = values(image, "RevLimit")
        assertEquals(listOf(6800.0), (Csv.readMap(Csv.writeMap(value), value) as TkResult.Success).data.values)
        assertTrue(Csv.readMap("a;b\n1;2", ignition) is TkResult.Failure)
        assertTrue(Csv.writeList(set, index).lines().first().startsWith("Name;Kind;Address"))
    }
}
