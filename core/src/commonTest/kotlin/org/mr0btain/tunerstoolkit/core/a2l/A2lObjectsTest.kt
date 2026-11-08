package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checklist: objects, axes, data types and the optional keywords */
class A2lObjectsTest {
    private fun axis(type: String, extra: String = "") =
        "/begin AXIS_DESCR $type rpm conv 8 0 8000 $extra /end AXIS_DESCR"

    @Test
    fun everyCharacteristicType() {
        val std = axis("STD_AXIS")
        val module = cleanModule(
            """
            /begin CHARACTERISTIC v "a value" VALUE 0x100 L 0 conv 0 100 /end CHARACTERISTIC
            /begin CHARACTERISTIC c "" CURVE 0x200 L 0 conv 0 100 $std /end CHARACTERISTIC
            /begin CHARACTERISTIC m "" MAP 0x300 L 0 conv 0 100 $std $std /end CHARACTERISTIC
            /begin CHARACTERISTIC q "" CUBOID 0x400 L 0 conv 0 100 $std $std $std /end CHARACTERISTIC
            /begin CHARACTERISTIC c4 "" CUBE_4 0x500 L 0 conv 0 100 $std $std $std $std /end CHARACTERISTIC
            /begin CHARACTERISTIC c5 "" CUBE_5 0x600 L 0 conv 0 100 $std $std $std $std $std /end CHARACTERISTIC
            /begin CHARACTERISTIC b "" VAL_BLK 0x700 L 0 conv 0 100 MATRIX_DIM 4 2 /end CHARACTERISTIC
            /begin CHARACTERISTIC t "" ASCII 0x800 L 0 NO_COMPU_METHOD 0 0 NUMBER 16 /end CHARACTERISTIC
            """
        )
        assertEquals(CharacteristicType.entries, module.characteristics.map { it.type })
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 0, 0), module.characteristics.map { it.axes.size })
        val value = module.characteristics[0]
        assertEquals("a value", value.longId)
        assertEquals(0x100L, value.address)
        assertEquals("L", value.recordLayout)
        assertEquals("conv", value.conversion)
        assertEquals(0.0, value.lowerLimit)
        assertEquals(100.0, value.upperLimit)
        assertEquals(listOf(4, 2), module.characteristic("b")?.options?.matrixDim)
        assertEquals(16, module.characteristic("t")?.options?.number)
    }

    @Test
    fun aMapWithTheWrongNumberOfAxesIsAWarning() {
        val (module, diagnostics) = readModule("/begin CHARACTERISTIC m \"\" MAP 0 L 0 c 0 1 ${axis("STD_AXIS")} /end CHARACTERISTIC")
        assertNotNull(module.characteristic("m"))
        assertTrue(diagnostics.messages(Severity.WARNING).single().contains("1 AXIS_DESCR, it should have 2"))
    }

    @Test
    fun brokenCharacteristicsAreLeftOutAndNamed() {
        val (module, diagnostics) = readModule(
            """
            /begin CHARACTERISTIC bad "" SPIRAL 0x0 L 0 c 0 1 /end CHARACTERISTIC
            /begin CHARACTERISTIC noaddress "" VALUE /end CHARACTERISTIC
            /begin CHARACTERISTIC good "" VALUE 0x10 L 0 c 0 1 /end CHARACTERISTIC
            """
        )
        assertEquals(listOf("good"), module.characteristics.map { it.name })
        val errors = diagnostics.filter { it.severity == Severity.ERROR }
        assertTrue(errors.any { it.objectName == "bad" })
        assertTrue(errors.any { it.objectName == "noaddress" && it.location?.line == 6 })
    }

    @Test
    fun everyAxisType() {
        val module = cleanModule(
            """
            /begin CHARACTERISTIC m "" CUBE_5 0 L 0 c 0 1
              ${axis("STD_AXIS", "DEPOSIT DIFFERENCE BYTE_ORDER MSB_FIRST MAX_GRAD 5 MONOTONY STRICT_INCREASE READ_ONLY")}
              ${axis("FIX_AXIS", "FIX_AXIS_PAR 0 4 6")}
              ${axis("FIX_AXIS", "FIX_AXIS_PAR_DIST 10 2.5 4")}
              ${axis("COM_AXIS", "AXIS_PTS_REF shared_x")}
              ${axis("CURVE_AXIS", "CURVE_AXIS_REF rescale_curve")}
            /end CHARACTERISTIC
            /begin CHARACTERISTIC r "" CURVE 0 L 0 c 0 1
              ${axis("RES_AXIS", "AXIS_PTS_REF res")}
            /end CHARACTERISTIC
            /begin CHARACTERISTIC l "" CURVE 0 L 0 c 0 1
              ${axis("FIX_AXIS", "/begin FIX_AXIS_PAR_LIST 1 2 4 8 /end FIX_AXIS_PAR_LIST")}
            /end CHARACTERISTIC
            """
        )
        val axes = module.characteristic("m")!!.axes
        assertEquals(listOf(AxisType.STD_AXIS, AxisType.FIX_AXIS, AxisType.FIX_AXIS, AxisType.COM_AXIS, AxisType.CURVE_AXIS), axes.map { it.type })
        val std = axes[0]
        assertEquals("rpm", std.inputQuantity)
        assertEquals("conv", std.conversion)
        assertEquals(8, std.maxAxisPoints)
        assertEquals(8000.0, std.upperLimit)
        assertEquals(Deposit.DIFFERENCE, std.deposit)
        assertEquals(ByteOrder.BIG_ENDIAN, std.options.byteOrder)
        assertEquals(5.0, std.options.maxGrad)
        assertEquals("STRICT_INCREASE", std.options.monotony)
        assertTrue(std.options.readOnly)
        val par = assertIs<FixAxis.Par>(axes[1].fixAxis)
        assertEquals(listOf(0.0, 4, 6), listOf(par.offset, par.shift, par.count))
        val dist = assertIs<FixAxis.ParDist>(axes[2].fixAxis)
        assertEquals(listOf(10.0, 2.5, 4.0), listOf(dist.offset, dist.distance, dist.count.toDouble()))
        assertEquals("shared_x", axes[3].axisPtsRef)
        assertEquals("rescale_curve", axes[4].curveAxisRef)
        assertEquals("res", module.characteristic("r")!!.axes.single().axisPtsRef)
        assertEquals(listOf(1.0, 2.0, 4.0, 8.0), assertIs<FixAxis.ParList>(module.characteristic("l")!!.axes.single().fixAxis).values)
    }

    @Test
    fun axesMissingWhatTheirTypeNeedsAreWarnings() {
        val (_, diagnostics) = readModule(
            "/begin CHARACTERISTIC m \"\" MAP 0 L 0 c 0 1 ${axis("FIX_AXIS")} ${axis("COM_AXIS")} /end CHARACTERISTIC"
        )
        assertEquals(2, diagnostics.count { it.severity == Severity.WARNING && it.objectName == "m" })
    }

    @Test
    fun axisPtsMeasurementBlobAndInstance() {
        val module = cleanModule(
            """
            /begin AXIS_PTS shared_x "rpm axis" 0x1000 rpm L_AXIS 0 conv 16 0 8000 DEPOSIT ABSOLUTE FORMAT "%5.0" /end AXIS_PTS
            /begin MEASUREMENT n "engine speed" UWORD conv 1 0.5 0 8000
              ECU_ADDRESS 0xD0001000 ARRAY_SIZE 2 ERROR_MASK 0x8000 BIT_MASK 0x7FFF
              /begin VIRTUAL a b /end VIRTUAL
            /end MEASUREMENT
            /begin BLOB raw "a blob" 0x2000 64 CALIBRATION_ACCESS CALIBRATION /end BLOB
            /begin INSTANCE inst "an instance" my_struct 0x3000 MATRIX_DIM 2 /end INSTANCE
            """
        )
        val axisPts = module.axisPts("shared_x")!!
        assertEquals(listOf("rpm axis", "rpm", "L_AXIS", "conv"), listOf(axisPts.longId, axisPts.inputQuantity, axisPts.recordLayout, axisPts.conversion))
        assertEquals(0x1000L, axisPts.address)
        assertEquals(16, axisPts.maxAxisPoints)
        assertEquals(Deposit.ABSOLUTE, axisPts.deposit)
        assertEquals("%5.0", axisPts.options.format)

        val measurement = module.measurement("n")!!
        assertEquals(DataType.UWORD, measurement.dataType)
        assertEquals(0.5, measurement.accuracy)
        assertEquals(0xD0001000L, measurement.ecuAddress)
        assertEquals(2, measurement.arraySize)
        assertEquals(0x8000L, measurement.errorMask)
        assertEquals(0x7FFFL, measurement.options.bitMask)
        assertEquals(listOf("a", "b"), measurement.virtual)

        val blob = module.blobs.single()
        assertEquals(listOf<Any>("raw", 0x2000L, 64L, "CALIBRATION"), listOf(blob.name, blob.address, blob.size, blob.options.calibrationAccess!!))
        val instance = module.instances.single()
        assertEquals(listOf<Any>("inst", "my_struct", 0x3000L), listOf(instance.name, instance.typedefName, instance.address))
    }

    @Test
    fun typedefsStructuresAndTransformers() {
        val module = cleanModule(
            """
            /begin TYPEDEF_CHARACTERISTIC T_map "" MAP L 0 c 0 1 /end TYPEDEF_CHARACTERISTIC
            /begin TYPEDEF_AXIS T_axis "" rpm L 0 c 8 0 8000 /end TYPEDEF_AXIS
            /begin TYPEDEF_MEASUREMENT T_meas "" UBYTE c 1 0 0 255 /end TYPEDEF_MEASUREMENT
            /begin TYPEDEF_BLOB T_blob "" 32 /end TYPEDEF_BLOB
            /begin TYPEDEF_STRUCTURE T_struct "" 16
              /begin STRUCTURE_COMPONENT gain T_meas 0 /end STRUCTURE_COMPONENT
              /begin STRUCTURE_COMPONENT table T_map 4 MATRIX_DIM 2 /end STRUCTURE_COMPONENT
            /end TYPEDEF_STRUCTURE
            /begin TRANSFORMER tr "1.0" "a.dll" "a64.dll" 2000 ON_CHANGE NO_INVERSE_TRANSFORMER
              /begin TRANSFORMER_IN_OBJECTS x y /end TRANSFORMER_IN_OBJECTS
              /begin TRANSFORMER_OUT_OBJECTS z /end TRANSFORMER_OUT_OBJECTS
            /end TRANSFORMER
            """
        )
        assertEquals(
            listOf("TYPEDEF_CHARACTERISTIC", "TYPEDEF_AXIS", "TYPEDEF_MEASUREMENT", "TYPEDEF_BLOB", "TYPEDEF_STRUCTURE"),
            module.typedefs.map { it.kind }
        )
        val components = module.typedefs.last().components
        assertEquals(listOf("gain", "table"), components.map { it.name })
        assertEquals(4L, components[1].offset)
        assertEquals(listOf(2), components[1].options.matrixDim)
        val transformer = module.transformers.single()
        assertEquals(listOf("tr", "a64.dll", "ON_CHANGE"), listOf(transformer.name, transformer.dll64, transformer.trigger))
        assertEquals(2000L, transformer.timeout)
        assertEquals(listOf("x", "y"), transformer.inObjects)
        assertEquals(listOf("z"), transformer.outObjects)
    }

    @Test
    fun everyDataType() {
        val names = DataType.entries.joinToString("\n") { type ->
            "/begin MEASUREMENT m_${type.name} \"\" ${type.name} c 0 0 0 1 /end MEASUREMENT"
        }
        val module = cleanModule(names)
        assertEquals(DataType.entries, module.measurements.map { it.dataType })
        assertEquals(listOf(1, 1, 2, 2, 4, 4, 8, 8, 2, 4, 8), DataType.entries.map { it.size })
        val (bad, diagnostics) = readModule("/begin MEASUREMENT x \"\" UINT128 c 0 0 0 1 /end MEASUREMENT")
        assertTrue(bad.measurements.isEmpty())
        assertTrue(diagnostics.any { it.severity == Severity.ERROR && it.objectName == "x" })
    }

    @Test
    fun theOptionalKeywords() {
        val module = cleanModule(
            """
            /begin CHARACTERISTIC c "" VALUE 0x10 L 0 conv 0 100
              BIT_MASK 0xF0
              /begin BIT_OPERATION RIGHT_SHIFT 4 SIGN_EXTEND /end BIT_OPERATION
              BYTE_ORDER MSB_FIRST
              MATRIX_DIM 1 1 1
              NUMBER 1
              EXTENDED_LIMITS -10 110
              READ_ONLY
              GUARD_RAILS
              DISCRETE
              DISPLAY_IDENTIFIER Shown_Name
              FORMAT "%6.2"
              PHYS_UNIT "Nm"
              MAX_REFRESH 3 10
              ECU_ADDRESS_EXTENSION 1
              CALIBRATION_ACCESS NO_CALIBRATION
              COMPARISON_QUANTITY other
              /begin DEPENDENT_CHARACTERISTIC "X1 * 2 + X2" a b /end DEPENDENT_CHARACTERISTIC
              /begin VIRTUAL_CHARACTERISTIC "sin(X1)" a /end VIRTUAL_CHARACTERISTIC
              SYMBOL_LINK "_sym" 4
              MODEL_LINK "model/path"
              ENCODING UTF8
              MONOTONY MON_INCREASE
              STEP_SIZE 0.5
              /begin ANNOTATION ANNOTATION_LABEL "Note" ANNOTATION_ORIGIN "Tuner"
                /begin ANNOTATION_TEXT "line 1" "line 2" /end ANNOTATION_TEXT
              /end ANNOTATION
              /begin FUNCTION_LIST f1 f2 /end FUNCTION_LIST
              /begin IF_DATA CANAPE_EXT 100 /end IF_DATA
            /end CHARACTERISTIC
            """
        )
        val options = module.characteristic("c")!!.options
        assertEquals(0xF0L, options.bitMask)
        assertEquals(4, options.bitOperation?.rightShift)
        assertNull(options.bitOperation?.leftShift)
        assertTrue(options.bitOperation!!.signExtend)
        assertEquals(ByteOrder.BIG_ENDIAN, options.byteOrder)
        assertEquals(listOf(1, 1, 1), options.matrixDim)
        assertEquals(1, options.number)
        assertEquals(-10.0 to 110.0, options.extendedLimits)
        assertTrue(options.readOnly && options.guardRails && options.discrete)
        assertEquals("Shown_Name", options.displayIdentifier)
        assertEquals("%6.2", options.format)
        assertEquals("Nm", options.physUnit)
        assertEquals(3L to 10L, options.maxRefresh)
        assertEquals(1L, options.ecuAddressExtension)
        assertEquals("NO_CALIBRATION", options.calibrationAccess)
        assertEquals("other", options.comparisonQuantity)
        assertEquals("X1 * 2 + X2", options.dependentCharacteristic?.expression)
        assertEquals(listOf("a", "b"), options.dependentCharacteristic?.inputs)
        assertEquals("sin(X1)", options.virtualCharacteristic?.expression)
        assertEquals("_sym" to 4L, options.symbolLink)
        assertEquals("model/path", options.modelLink)
        assertEquals("UTF8", options.encoding)
        assertEquals("MON_INCREASE", options.monotony)
        assertEquals(0.5, options.stepSize)
        val annotation = options.annotations.single()
        assertEquals(listOf("Note", "Tuner"), listOf(annotation.label, annotation.origin))
        assertEquals(listOf("line 1", "line 2"), annotation.text)
        assertEquals(listOf("f1", "f2"), options.functionList)
        assertEquals("CANAPE_EXT", options.ifData.single().tag)
    }

    @Test
    fun duplicateNamesAreWarnedAndTheFirstWins() {
        val (module, diagnostics) = readModule(
            """
            /begin CHARACTERISTIC c "first" VALUE 0x10 L 0 conv 0 100 /end CHARACTERISTIC
            /begin CHARACTERISTIC c "second" VALUE 0x20 L 0 conv 0 100 /end CHARACTERISTIC
            """
        )
        assertEquals("first", module.characteristic("c")?.longId)
        assertTrue(diagnostics.any { "defined 2 times" in it.message })
    }
}
