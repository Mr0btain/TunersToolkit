package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordBytes
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class A2lResolverTest {
    private fun resolve(body: String): CalibrationSet = A2lResolver(readModule(body).first).resolve()

    /** Resolves [body] and checks nothing went wrong */
    private fun clean(body: String): CalibrationSet {
        val set = resolve(body)
        assertEquals(emptyList(), set.diagnostics.filter { it.severity != Severity.INFO }.map { it.toString() })
        return set
    }

    private fun CalibrationSet.read(image: MemoryImage, name: String): CalibrationValues {
        val obj = this[name] ?: fail("$name wasn't resolved: $diagnostics")
        return when (val result = decode(image, obj)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> fail(result.error.message)
        }
    }

    private val common = """
        /begin MOD_COMMON "" BYTE_ORDER MSB_LAST ALIGNMENT_BYTE 1 ALIGNMENT_WORD 2 /end MOD_COMMON
        /begin RECORD_LAYOUT Val_UB FNC_VALUES 1 UBYTE ROW_DIR DIRECT /end RECORD_LAYOUT
        /begin RECORD_LAYOUT Val_UW FNC_VALUES 1 UWORD ROW_DIR DIRECT /end RECORD_LAYOUT
    """

    @Test
    fun everyDemoObjectReadsBackWhatWasWritten() {
        val demo = DemoEcu.build()
        val imported = readA2l(demo.a2l)
        val set = A2lResolver(imported.value.modules.single()).resolve()
        assertEquals(emptyList(), set.diagnostics.filter { it.severity != Severity.INFO }.map { it.toString() })
        assertEquals(demo.placed.keys, set.objects.map { it.name }.toSet())
        for ((name, placed) in demo.placed) {
            val obj = set[name]!!
            val values = set.read(demo.image, name)
            val lsb = abs(obj.conversion.toPhysical(1.0) - obj.conversion.toPhysical(0.0))
            val expected = when (obj.kind) {
                ObjectKind.ASCII -> {
                    assertEquals("TTDEMO1-0001", values.text)
                    continue
                }
                // The demo writes maps x by x (y first), the codec gives them x first
                ObjectKind.MAP -> {
                    val (nx, ny) = values.dimensions.toList()
                    List(nx * ny) { i -> placed.values[(i % nx) * ny + i / nx] }
                }
                else -> placed.values
            }
            assertEquals(expected.size, values.size, name)
            expected.forEachIndexed { i, value ->
                assertTrue(abs(values.physical[i] - value) <= lsb / 2 + 1e-9, "$name[$i]: ${values.physical[i]} isn't $value")
            }
        }
        assertEquals(DemoEcu.rpmAxis, set.read(demo.image, "BoostTarget").axes[0].physical.toList())
        // Stored in 0.75 degree steps, so within half of one
        val temps = set.read(demo.image, "WarmupEnrichment").axes[0].physical
        DemoEcu.tempAxis.forEachIndexed { i, t -> assertTrue(abs(temps[i] - t) <= 0.375, "${temps[i]} isn't $t") }
        assertEquals((1..6).map(Int::toDouble), set.read(demo.image, "GearTorqueLimit").axes[0].physical.toList())
        assertEquals("Off", set.read(demo.image, "LaunchControl").textAt(0))
    }

    @Test
    fun characteristicsAndTheirSettings() {
        val set = clean(
            """
            $common
            /begin COMPU_METHOD Temp "" LINEAR "%5.1" "degC" COEFFS_LINEAR 0.75 -48 /end COMPU_METHOD
            /begin CHARACTERISTIC FanOn "Fan on" VALUE 0x1000 Val_UB 0 Temp -48 143.25
              FORMAT "%4.0" DISPLAY_IDENTIFIER fan_on READ_ONLY BIT_MASK 0x7F EXTENDED_LIMITS -50 150
            /end CHARACTERISTIC
            /begin CHARACTERISTIC Trims "" VAL_BLK 0x1002 Val_UW 0 NO_COMPU_METHOD 0 65535 MATRIX_DIM 4 2 1 /end CHARACTERISTIC
            /begin CHARACTERISTIC Id "" ASCII 0x1020 Val_UB 0 NO_COMPU_METHOD 0 255 NUMBER 12 /end CHARACTERISTIC
            /begin CHARACTERISTIC Gains "" VAL_BLK 0x1030 Val_UB 0 NO_COMPU_METHOD 0 255 NUMBER 3 BYTE_ORDER MSB_FIRST PHYS_UNIT "-" /end CHARACTERISTIC
            """
        )
        val fan = set["FanOn"]!!
        assertEquals(ObjectKind.VALUE, fan.kind)
        assertEquals(0x1000L, fan.address)
        assertEquals(Conversion.Linear(0.75, -48.0), fan.conversion)
        assertEquals("degC", fan.unit)
        assertEquals("%4.0", fan.format, "the object's FORMAT wins over the method's")
        assertEquals("fan_on", fan.displayName)
        assertTrue(fan.readOnly)
        assertEquals(0x7FL, fan.bitMask)
        assertEquals(-50.0..150.0, fan.extendedLimits)
        assertEquals(-48.0 to 143.25, fan.lowerLimit to fan.upperLimit)
        assertEquals(ByteOrder.LITTLE_ENDIAN, fan.storage.byteOrder)
        assertEquals(listOf(4, 2), set["Trims"]!!.dimensions, "MATRIX_DIM's trailing 1 dropped")
        assertEquals(listOf(12), set["Id"]!!.dimensions)
        assertEquals(ByteOrder.BIG_ENDIAN, set["Gains"]!!.storage.byteOrder, "the object's BYTE_ORDER wins")
        assertEquals("-", set["Gains"]!!.unit)
        assertEquals(1, set["FanOn"]!!.storage.alignment(DataType.UBYTE))
    }

    @Test
    fun recordLayoutFieldsAndAlignment() {
        val set = clean(
            """
            $common
            /begin RECORD_LAYOUT Curve_Full
              NO_AXIS_PTS_X 1 UBYTE
              SRC_ADDR_X 2 ULONG
              AXIS_PTS_X 3 SWORD INDEX_DECR DIRECT
              RIP_ADDR_X 4 UWORD
              RESERVED 5 LONG
              FNC_VALUES 6 FLOAT32_IEEE COLUMN_DIR PLONG
              IDENTIFICATION 7 UWORD
              ALIGNMENT_LONG 2
              STATIC_RECORD_LAYOUT
            /end RECORD_LAYOUT
            /begin CHARACTERISTIC C "" CURVE 0x2000 Curve_Full 0 NO_COMPU_METHOD 0 1
              /begin AXIS_DESCR STD_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 8 0 100 /end AXIS_DESCR
            /end CHARACTERISTIC
            """
        )
        val storage = set["C"]!!.storage
        val fields = storage.fields
        assertEquals(7, fields.size)
        assertIs<RecordField.AxisCount>(fields[0])
        assertEquals("SRC_ADDR_X", (fields[1] as RecordField.Skip).what)
        assertTrue((fields[2] as RecordField.AxisPoints).decreasing)
        assertEquals(DataType.ULONG, (fields[4] as RecordField.Skip).type)
        val values = fields[5] as RecordField.Values
        assertEquals(ValueOrder.Y_FIRST, values.order)
        assertEquals(DataType.ULONG, values.pointer)
        assertTrue(storage.static)
        assertEquals(2, storage.alignment(DataType.ULONG), "the layout's ALIGNMENT_LONG")
        assertEquals(2, storage.alignment(DataType.UWORD), "MOD_COMMON's ALIGNMENT_WORD")
        assertEquals(4, storage.alignment(DataType.FLOAT32_IEEE), "not given: its size")
    }

    @Test
    fun axesOfEveryType() {
        val set = clean(
            """
            $common
            /begin RECORD_LAYOUT Axis_UB NO_AXIS_PTS_X 1 UBYTE AXIS_PTS_X 2 UBYTE INDEX_INCR DIRECT /end RECORD_LAYOUT
            /begin RECORD_LAYOUT Rescale_UB NO_RESCALE_X 1 UBYTE AXIS_RESCALE_X 2 UBYTE 4 INDEX_INCR DIRECT /end RECORD_LAYOUT
            /begin RECORD_LAYOUT Cube FIX_NO_AXIS_PTS_X 2 AXIS_PTS_X 1 UBYTE INDEX_INCR DIRECT FNC_VALUES 2 UBYTE ROW_DIR DIRECT /end RECORD_LAYOUT
            /begin AXIS_PTS Speed "" 0x3000 EngineSpeed Axis_UB 0 NO_COMPU_METHOD 6 0 255 DEPOSIT DIFFERENCE /end AXIS_PTS
            /begin AXIS_PTS Res "" 0x3010 NO_INPUT_QUANTITY Rescale_UB 0 NO_COMPU_METHOD 4 0 255 /end AXIS_PTS
            /begin CHARACTERISTIC Lookup "" CURVE 0x3020 Val_UB 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR FIX_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 3 0 10 FIX_AXIS_PAR_DIST 0 5 3 /end AXIS_DESCR
            /end CHARACTERISTIC
            /begin CHARACTERISTIC Cube "" CUBE_5 0x3100 Cube 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR STD_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 10 /end AXIS_DESCR
              /begin AXIS_DESCR COM_AXIS EngineSpeed NO_COMPU_METHOD 6 0 255 AXIS_PTS_REF Speed /end AXIS_DESCR
              /begin AXIS_DESCR RES_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 5 0 255 AXIS_PTS_REF Res /end AXIS_DESCR
              /begin AXIS_DESCR FIX_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 10 FIX_AXIS_PAR 1 2 2 /end AXIS_DESCR
              /begin AXIS_DESCR CURVE_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 3 0 10 CURVE_AXIS_REF Lookup /end AXIS_DESCR
            /end CHARACTERISTIC
            """
        )
        val speed = set["Speed"]!!
        assertEquals(ObjectKind.AXIS, speed.kind)
        assertEquals("EngineSpeed", speed.axes.single().input)
        assertTrue(speed.axes.single().difference)
        val cube = set["Cube"]!!
        assertEquals(ObjectKind.CUBE_5, cube.kind)
        assertEquals(2, cube.axes[0].fixedCount)
        assertEquals("Speed", (cube.axes[1].source as AxisSource.Shared).name)
        assertEquals("Res", (cube.axes[2].source as AxisSource.Rescaled).name)
        assertEquals(listOf(1.0, 5.0), (cube.axes[3].source as AxisSource.Fixed).points, "offset 1, 2^2 apart")
        assertEquals("Lookup", (cube.axes[4].source as AxisSource.Curve).name)
        assertEquals(listOf(0.0, 5.0, 10.0), (set["Lookup"]!!.axes.single().source as AxisSource.Fixed).points)

        // Read it: x has 2 points in its record, y 2 shared, z 5 rescaled, the 4th 2 fixed, the 5th 3 indexes
        val image = MemoryImage.of(
            0x3000L to byteArrayOf(2, 10, 5),
            0x3010L to byteArrayOf(2, 0, 0, 40, 4),
            0x3100L to ByteArray(2 + 2 * 2 * 5 * 2 * 3) { (it - 2).toByte() }.also { it[0] = 7; it[1] = 8 }
        )
        val values = set.read(image, "Cube")
        assertEquals(listOf(2, 2, 5, 2, 3), values.dimensions.toList())
        assertEquals(listOf(7.0, 8.0), values.axes[0].raw.toList())
        assertEquals(listOf(10.0, 15.0), values.axes[1].raw.toList(), "DEPOSIT DIFFERENCE")
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0), values.axes[2].raw.toList())
        assertEquals(119.0, values[1, 1, 4, 1, 2], "the last value")
    }

    @Test
    fun calibrationOffsetAndVariants() {
        val set = clean(
            """
            $common
            /begin MOD_PAR "" ECU_CALIBRATION_OFFSET 0x10000 /end MOD_PAR
            /begin CHARACTERISTIC Limit "" VALUE 0x1000 Val_UB 0 NO_COMPU_METHOD 0 255 /end CHARACTERISTIC
            /begin VARIANT_CODING
              /begin VAR_CRITERION Gearbox "" Manual Auto /end VAR_CRITERION
              /begin VAR_CHARACTERISTIC Limit Gearbox /begin VAR_ADDRESS 0x1000 0x1004 /end VAR_ADDRESS /end VAR_CHARACTERISTIC
            /end VARIANT_CODING
            """
        )
        val limit = set["Limit"]!!
        assertEquals(0x11000L, limit.address)
        assertEquals(listOf(0x11000L, 0x11004L), limit.variantAddresses)
        val image = MemoryImage.of(0x11000L to byteArrayOf(10, 0, 0, 0, 20))
        assertEquals(20.0, (set.decode(image, limit, variant = 1) as TkResult.Success).data.raw.single())
        assertTrue((set.decode(image, limit, variant = 2) as TkResult.Failure).error.message.contains("no variant 2"))
    }

    @Test
    fun everyConversionType() {
        val set = clean(
            """
            $common
            /begin MOD_PAR "" SYSTEM_CONSTANT "SCALE" "4" /end MOD_PAR
            /begin COMPU_METHOD Ident "" IDENTICAL "%5.0" "" /end COMPU_METHOD
            /begin COMPU_METHOD Rat "" RAT_FUNC "%5.2" "" COEFFS 0 4 8 0 0 2 /end COMPU_METHOD
            /begin COMPU_METHOD Quad "" RAT_FUNC "%5.2" "" COEFFS 1 0 0 0 0 1 /end COMPU_METHOD
            /begin COMPU_METHOD Form "" FORM "%5.2" "" /begin FORMULA "X1 * sysc(SCALE)" FORMULA_INV "X1 / sysc(SCALE)" /end FORMULA /end COMPU_METHOD
            /begin COMPU_METHOD Tab "" TAB_INTP "%5.2" "" COMPU_TAB_REF Tab /end COMPU_METHOD
            /begin COMPU_TAB Tab "" TAB_INTP 2 0 0 10 100 DEFAULT_VALUE_NUMERIC -1 /end COMPU_TAB
            /begin COMPU_METHOD NoIntp "" TAB_NOINTP "%5.2" "" COMPU_TAB_REF Tab /end COMPU_METHOD
            /begin COMPU_METHOD Mode "" TAB_VERB "%3.0" "" COMPU_TAB_REF ModeRange /end COMPU_METHOD
            /begin COMPU_VTAB_RANGE ModeRange "" 2 0 9 "Low" 10 255 "High" DEFAULT_VALUE "?" /end COMPU_VTAB_RANGE
            /begin COMPU_METHOD Volts "" LINEAR "%5.2" "V" COEFFS_LINEAR 0.1 0 STATUS_STRING_REF Sna /end COMPU_METHOD
            /begin COMPU_VTAB Sna "" TAB_VERB 1 255 "SNA" /end COMPU_VTAB
            ${listOf("Ident", "Rat", "Quad", "Form", "Tab", "NoIntp", "Mode", "Volts").joinToString("\n") {
                "/begin CHARACTERISTIC C_$it \"\" VALUE 0x1000 Val_UB 0 $it 0 255 /end CHARACTERISTIC"
            }}
            """
        )
        fun conversion(name: String) = set["C_$name"]!!.conversion
        assertEquals(Conversion.Identical, conversion("Ident"))
        // raw = (4 p + 8) / 2 = 2 p + 4, so p = raw / 2 - 2
        assertEquals(Conversion.Linear(0.5, -2.0), conversion("Rat"))
        assertEquals(3.0, conversion("Quad").toPhysical(9.0), "raw = p²")
        assertEquals(12.0, conversion("Form").toPhysical(3.0))
        assertEquals(3.0, conversion("Form").toRaw(12.0))
        assertEquals(50.0, conversion("Tab").toPhysical(5.0))
        assertEquals(-1.0, conversion("NoIntp").toPhysical(5.0), "no exact match: the default")
        assertEquals("High", conversion("Mode").text(200.0), "COMPU_VTAB_RANGE, not taken for a COMPU_VTAB")
        assertEquals("?", conversion("Mode").text(-1.0))
        assertEquals(12.0, conversion("Volts").toPhysical(120.0))
        assertEquals("SNA", conversion("Volts").text(255.0))
        assertNull(conversion("Volts").text(254.0))
    }

    @Test
    fun brokenObjectsAreLeftOutAndNamed() {
        val set = resolve(
            """
            $common
            /begin RECORD_LAYOUT Alt FNC_VALUES 1 UBYTE ALTERNATE_WITH_X DIRECT /end RECORD_LAYOUT
            /begin RECORD_LAYOUT Points AXIS_PTS_Y 1 UBYTE INDEX_INCR DIRECT FNC_VALUES 2 UBYTE ROW_DIR DIRECT /end RECORD_LAYOUT
            /begin COMPU_METHOD BadForm "" FORM "%5.2" "" /begin FORMULA "X1 +" /end FORMULA /end COMPU_METHOD
            /begin CHARACTERISTIC NoLayout "" VALUE 0x1000 Missing 0 NO_COMPU_METHOD 0 255 /end CHARACTERISTIC
            /begin CHARACTERISTIC Alternate "" VALUE 0x1000 Alt 0 NO_COMPU_METHOD 0 255 /end CHARACTERISTIC
            /begin CHARACTERISTIC WrongAxis "" CURVE 0x1000 Points 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR FIX_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 1 FIX_AXIS_PAR_LIST 0 1 /end AXIS_DESCR
            /end CHARACTERISTIC
            /begin CHARACTERISTIC NotStored "" CURVE 0x1000 Val_UB 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR STD_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 1 /end AXIS_DESCR
            /end CHARACTERISTIC
            /begin CHARACTERISTIC NoShared "" CURVE 0x1000 Val_UB 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR COM_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 1 AXIS_PTS_REF Nowhere /end AXIS_DESCR
            /end CHARACTERISTIC
            /begin CHARACTERISTIC BadFormula "" VALUE 0x1000 Val_UB 0 BadForm 0 255 /end CHARACTERISTIC
            /begin CHARACTERISTIC AlsoBadFormula "" VALUE 0x1000 Val_UB 0 BadForm 0 255 /end CHARACTERISTIC
            /begin CHARACTERISTIC UnknownMethod "" VALUE 0x1000 Val_UB 0 Nope 0 255 /end CHARACTERISTIC
            /begin CHARACTERISTIC Fine "" VALUE 0x1000 Val_UB 0 NO_COMPU_METHOD 0 255 /end CHARACTERISTIC
            """
        )
        assertEquals(listOf("UnknownMethod", "Fine"), set.objects.map { it.name })
        assertEquals(Conversion.Identical, set["UnknownMethod"]!!.conversion)
        val errors = set.diagnostics.filter { it.severity == Severity.ERROR }
        assertEquals(
            listOf("NoLayout", "Alternate", "WrongAxis", "NotStored", "NoShared", "BadForm", "BadFormula", "AlsoBadFormula"),
            errors.map { it.objectName }
        )
        assertTrue(errors.all { it.location != null })
        assertTrue("ALTERNATE_WITH_X" in errors[1].message)
        assertTrue("Nowhere" in errors[4].message)
        assertTrue(set.diagnostics.any { it.severity == Severity.WARNING && it.objectName == "UnknownMethod" })
    }

    @Test
    fun countsForAxesTheObjectDoesntHaveAreSkippedOver() {
        val set = resolve(
            """
            $common
            /begin RECORD_LAYOUT Shared NO_AXIS_PTS_X 1 UBYTE NO_AXIS_PTS_Y 2 UBYTE FNC_VALUES 3 UBYTE ROW_DIR DIRECT /end RECORD_LAYOUT
            /begin CHARACTERISTIC C "" CURVE 0x1000 Shared 0 NO_COMPU_METHOD 0 255
              /begin AXIS_DESCR FIX_AXIS NO_INPUT_QUANTITY NO_COMPU_METHOD 2 0 1 FIX_AXIS_PAR_LIST 0 1 /end AXIS_DESCR
            /end CHARACTERISTIC
            """
        )
        assertEquals("NO_AXIS_PTS_Y", (set["C"]!!.storage.fields[1] as RecordField.Skip).what)
        assertTrue(set.diagnostics.single().message.contains("NO_AXIS_PTS_Y"))
        // The X count is still read, but the fixed axis decides the curve's size
        val image = RecordBytes().put(DataType.UBYTE, 2, 9, 5, 6).image()
        assertEquals(listOf(5.0, 6.0), (CalibrationCodec.decode(image, set["C"]!!, set) as TkResult.Success).data.raw.toList())
    }

    @Test
    fun missingByteOrderIsNotedOnce() {
        val set = resolve(
            """
            /begin RECORD_LAYOUT Val_UW FNC_VALUES 1 UWORD ROW_DIR DIRECT /end RECORD_LAYOUT
            /begin CHARACTERISTIC A "" VALUE 0x1000 Val_UW 0 NO_COMPU_METHOD 0 65535 /end CHARACTERISTIC
            /begin CHARACTERISTIC B "" VALUE 0x1002 Val_UW 0 NO_COMPU_METHOD 0 65535 /end CHARACTERISTIC
            """
        )
        assertEquals(ByteOrder.BIG_ENDIAN, set["A"]!!.storage.byteOrder)
        assertEquals(1, set.diagnostics.count { "BYTE_ORDER" in it.message && it.severity == Severity.INFO })
        val obj: CalibrationObject = assertNotNull(set["B"])
        assertEquals(0x1002L, obj.address)
    }
}
