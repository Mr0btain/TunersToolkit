package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Checklist: RECORD_LAYOUT, conversions, units and organisation */
class A2lLayoutAndConversionTest {
    @Test
    fun recordLayoutWithEveryKeyword() {
        val axes = listOf("X", "Y", "Z", "4", "5")
        val perAxis = axes.mapIndexed { index, axis ->
            val base = 10 + index * 10
            """
            NO_AXIS_PTS_$axis ${base} UWORD
            AXIS_PTS_$axis ${base + 1} SWORD INDEX_INCR DIRECT
            AXIS_RESCALE_$axis ${base + 2} UBYTE 5 INDEX_DECR DIRECT
            NO_RESCALE_$axis ${base + 3} UBYTE
            SRC_ADDR_$axis ${base + 4} ULONG
            RIP_ADDR_$axis ${base + 5} UWORD
            SHIFT_OP_$axis ${base + 6} UBYTE
            OFFSET_$axis ${base + 7} SWORD
            DIST_OP_$axis ${base + 8} SWORD
            FIX_NO_AXIS_PTS_$axis ${index + 2}
            """
        }.joinToString("\n")
        val module = cleanModule(
            """
            /begin RECORD_LAYOUT Everything
              IDENTIFICATION 1 ULONG
              FNC_VALUES 2 FLOAT32_IEEE COLUMN_DIR PLONG
              RIP_ADDR_W 3 UWORD
              RESERVED 4 WORD
              $perAxis
              ALIGNMENT_BYTE 1 ALIGNMENT_WORD 2 ALIGNMENT_LONG 4 ALIGNMENT_INT64 8 ALIGNMENT_FLOAT16_IEEE 2 ALIGNMENT_FLOAT32_IEEE 4 ALIGNMENT_FLOAT64_IEEE 8
              STATIC_RECORD_LAYOUT
              STATIC_ADDRESS_OFFSETS
            /end RECORD_LAYOUT
            """
        )
        val layout = module.recordLayout("Everything")!!
        assertTrue(layout.staticRecordLayout && layout.staticAddressOffsets)
        assertEquals(7, layout.alignments.size)
        // In position order, the fixed counts (no position) first
        val positioned = layout.entries.filterNot { it is LayoutEntry.FixNoAxisPts }
        assertEquals(positioned.map { it.position }.sorted(), positioned.map { it.position })
        assertEquals(4 + 9 * 5, positioned.size)

        val fnc = assertNotNull(layout.fncValues)
        assertEquals(DataType.FLOAT32_IEEE, fnc.dataType)
        assertEquals(IndexMode.COLUMN_DIR, fnc.indexMode)
        assertEquals(AddressType.PLONG, fnc.addressType)
        assertEquals(DataType.ULONG, layout.entries.filterIsInstance<LayoutEntry.Identification>().single().dataType)
        assertEquals("W", layout.entries.filterIsInstance<LayoutEntry.RipAddr>().first().axis)
        assertEquals("WORD", layout.entries.filterIsInstance<LayoutEntry.Reserved>().single().dataSize)

        for ((index, axis) in axes.withIndex()) {
            assertEquals(DataType.SWORD, layout.axisPts(axis)?.dataType, axis)
            assertEquals(IndexOrder.INDEX_INCR, layout.axisPts(axis)?.indexOrder)
            assertEquals(DataType.UWORD, layout.noAxisPts(axis)?.dataType)
            assertEquals(index + 2, layout.fixNoAxisPts(axis)?.count)
            val rescale = layout.entries.filterIsInstance<LayoutEntry.AxisRescale>().single { it.axis == axis }
            assertEquals(5, rescale.maxPairs)
            assertEquals(IndexOrder.INDEX_DECR, rescale.indexOrder)
            listOf(
                layout.entries.filterIsInstance<LayoutEntry.NoRescale>(),
                layout.entries.filterIsInstance<LayoutEntry.SrcAddr>(),
                layout.entries.filterIsInstance<LayoutEntry.ShiftOp>(),
                layout.entries.filterIsInstance<LayoutEntry.Offset>(),
                layout.entries.filterIsInstance<LayoutEntry.DistOp>()
            ).forEach { entries -> assertEquals(5, entries.size) }
        }
    }

    @Test
    fun clashingPositionsAreAWarning() {
        val (_, diagnostics) = readModule("/begin RECORD_LAYOUT L NO_AXIS_PTS_X 1 UBYTE AXIS_PTS_X 1 UBYTE INDEX_INCR DIRECT /end RECORD_LAYOUT")
        assertTrue(diagnostics.single().message.contains("more than one entry at position 1"))
    }

    @Test
    fun everyConversionType() {
        val module = cleanModule(
            """
            /begin COMPU_METHOD id "" IDENTICAL "%3.0" "" /end COMPU_METHOD
            /begin COMPU_METHOD lin "" LINEAR "%5.2" "kPa" COEFFS_LINEAR 0.1 -10 /end COMPU_METHOD
            /begin COMPU_METHOD rat "" RAT_FUNC "%5.2" "Nm" COEFFS 0 1 0 0 0 0.5 REF_UNIT nm_unit /end COMPU_METHOD
            /begin COMPU_METHOD form "" FORM "%5.2" "V" /begin FORMULA "X1 * 5 / 1024" FORMULA_INV "X1 * 1024 / 5" /end FORMULA /end COMPU_METHOD
            /begin COMPU_METHOD intp "" TAB_INTP "%5.2" "" COMPU_TAB_REF tab /end COMPU_METHOD
            /begin COMPU_METHOD nointp "" TAB_NOINTP "%5.2" "" COMPU_TAB_REF tab /end COMPU_METHOD
            /begin COMPU_METHOD verb "" TAB_VERB "%5.2" "" COMPU_TAB_REF onoff STATUS_STRING_REF status /end COMPU_METHOD
            """
        )
        assertEquals(ConversionType.entries, module.compuMethods.map { it.type })
        assertEquals(0.1 to -10.0, module.compuMethod("lin")?.coeffsLinear)
        assertEquals(listOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.5), module.compuMethod("rat")?.coeffs)
        assertEquals("nm_unit", module.compuMethod("rat")?.refUnit)
        assertEquals("X1 * 5 / 1024", module.compuMethod("form")?.formula?.expression)
        assertEquals("X1 * 1024 / 5", module.compuMethod("form")?.formula?.inverse)
        assertEquals("tab", module.compuMethod("intp")?.compuTabRef)
        assertEquals("status", module.compuMethod("verb")?.statusStringRef)
        assertEquals("kPa", module.compuMethod("lin")?.unit)
        assertEquals("%5.2", module.compuMethod("lin")?.format)
    }

    @Test
    fun conversionsMissingTheirParametersAreWarnings() {
        val (_, diagnostics) = readModule(
            """
            /begin COMPU_METHOD a "" RAT_FUNC "" "" /end COMPU_METHOD
            /begin COMPU_METHOD b "" LINEAR "" "" /end COMPU_METHOD
            /begin COMPU_METHOD c "" FORM "" "" /end COMPU_METHOD
            /begin COMPU_METHOD d "" TAB_VERB "" "" /end COMPU_METHOD
            """
        )
        assertEquals(listOf("a", "b", "c", "d"), diagnostics.filter { it.severity == Severity.WARNING }.map { it.objectName })
    }

    @Test
    fun tablesVerbalTablesAndRanges() {
        val module = cleanModule(
            """
            /begin COMPU_TAB tab "" TAB_INTP 3 0 0 128 50.5 255 100 DEFAULT_VALUE_NUMERIC -1 /end COMPU_TAB
            /begin COMPU_VTAB onoff "" TAB_VERB 2 0 "Off" 1 "On" DEFAULT_VALUE "Unknown" /end COMPU_VTAB
            /begin COMPU_VTAB_RANGE gears "" 3 0 0 "Neutral" 1 6 "Forward" -1 -1 "Reverse" DEFAULT_VALUE "?" /end COMPU_VTAB_RANGE
            """
        )
        val tab = module.compuTab("tab")!!
        assertEquals(listOf(0.0 to 0.0, 128.0 to 50.5, 255.0 to 100.0), tab.pairs)
        assertEquals(-1.0, tab.defaultNumeric)
        val vtab = module.compuVTab("onoff")!!
        assertEquals(listOf(0.0 to "Off", 1.0 to "On"), vtab.entries)
        assertEquals("Unknown", vtab.defaultValue)
        // The old line parser read every COMPU_VTAB_RANGE as a COMPU_VTAB
        assertTrue(module.compuVTabs.none { it.name == "gears" })
        val range = module.compuVTabRange("gears")!!
        assertEquals(listOf("Neutral", "Forward", "Reverse"), range.entries.map { it.text })
        assertEquals(1.0 to 6.0, range.entries[1].min to range.entries[1].max)
        assertEquals("?", range.defaultValue)
    }

    @Test
    fun aTableWithTheWrongCountIsAWarning() {
        val (module, diagnostics) = readModule("/begin COMPU_VTAB v \"\" TAB_VERB 3 0 \"a\" 1 \"b\" /end COMPU_VTAB")
        assertEquals(2, module.compuVTab("v")?.entries?.size)
        assertTrue(diagnostics.single().message.contains("says it has 3 entries but has 2"))
    }

    @Test
    fun units() {
        val module = cleanModule(
            """
            /begin UNIT metre "length" "m" EXTENDED_SI SI_EXPONENTS 1 0 0 0 0 0 0 /end UNIT
            /begin UNIT km "" "km" DERIVED REF_UNIT metre UNIT_CONVERSION 1000 0 /end UNIT
            """
        )
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), module.unit("metre")?.siExponents)
        assertEquals("metre", module.unit("km")?.refUnit)
        assertEquals(1000.0 to 0.0, module.unit("km")?.conversion)
        assertEquals("DERIVED", module.unit("km")?.type)
    }

    @Test
    fun functionsGroupsFramesAndUserRights() {
        val module = cleanModule(
            """
            /begin FUNCTION Idle "idle control" FUNCTION_VERSION "1.2"
              /begin DEF_CHARACTERISTIC a b /end DEF_CHARACTERISTIC
              /begin REF_CHARACTERISTIC c /end REF_CHARACTERISTIC
              /begin IN_MEASUREMENT n /end IN_MEASUREMENT
              /begin OUT_MEASUREMENT q /end OUT_MEASUREMENT
              /begin LOC_MEASUREMENT tmp /end LOC_MEASUREMENT
              /begin SUB_FUNCTION IdleFast /end SUB_FUNCTION
            /end FUNCTION
            /begin GROUP Engine "" ROOT
              /begin REF_CHARACTERISTIC a /end REF_CHARACTERISTIC
              /begin REF_MEASUREMENT n /end REF_MEASUREMENT
              /begin SUB_GROUP Idle_Group /end SUB_GROUP
              /begin FUNCTION_LIST Idle /end FUNCTION_LIST
            /end GROUP
            /begin FRAME fr "" 3 10 FRAME_MEASUREMENT n q /end FRAME
            /begin USER_RIGHTS calibrator /begin REF_GROUP Engine /end REF_GROUP READ_ONLY /end USER_RIGHTS
            """
        )
        val function = module.functions.single()
        assertEquals("1.2", function.version)
        assertEquals(listOf("a", "b"), function.defCharacteristics)
        assertEquals(listOf("c"), function.refCharacteristics)
        assertEquals(listOf("n", "q", "tmp", "IdleFast"), function.inMeasurements + function.outMeasurements + function.locMeasurements + function.subFunctions)
        val group = module.groups.single()
        assertTrue(group.root)
        assertEquals(listOf("a", "n", "Idle_Group", "Idle"), group.refCharacteristics + group.refMeasurements + group.subGroups + group.functions)
        assertEquals(listOf("n", "q"), module.frames.single().measurements)
        val rights = module.userRights.single()
        assertEquals(listOf(listOf("Engine")), rights.refGroups)
        assertTrue(rights.readOnly)
    }

    @Test
    fun variantCoding() {
        val module = cleanModule(
            """
            /begin VARIANT_CODING
              VAR_SEPARATOR "." VAR_NAMING NUMERIC
              /begin VAR_CRITERION Car "body style" Limousine Kombi Cabrio VAR_MEASUREMENT body VAR_SELECTION_CHARACTERISTIC body_sel /end VAR_CRITERION
              /begin VAR_CRITERION Gear "" Manual Auto /end VAR_CRITERION
              /begin VAR_FORBIDDEN_COMB Car Cabrio Gear Auto /end VAR_FORBIDDEN_COMB
              /begin VAR_CHARACTERISTIC idle_rpm Car Gear
                /begin VAR_ADDRESS 0x1000 0x1004 0x1008 0x100C 0x1010 /end VAR_ADDRESS
              /end VAR_CHARACTERISTIC
            /end VARIANT_CODING
            """
        )
        val coding = assertNotNull(module.variantCoding)
        assertEquals("NUMERIC", coding.naming)
        assertEquals(listOf("Limousine", "Kombi", "Cabrio"), coding.criteria[0].values)
        assertEquals("body", coding.criteria[0].measurement)
        assertEquals("body_sel", coding.criteria[0].selectionCharacteristic)
        assertEquals(listOf(listOf("Car" to "Cabrio", "Gear" to "Auto")), coding.forbiddenCombinations)
        val characteristic = coding.characteristics.single()
        assertEquals(listOf("Car", "Gear"), characteristic.criteria)
        assertEquals(5, characteristic.addresses.size)
        assertIs<Long>(characteristic.addresses.first())
    }
}
