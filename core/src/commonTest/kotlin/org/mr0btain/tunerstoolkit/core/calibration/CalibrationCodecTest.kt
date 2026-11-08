package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Writes a record byte by byte, so each test lays out exactly what the A2L would describe */
internal class RecordBytes(val order: ByteOrder = ByteOrder.LITTLE_ENDIAN, val start: Long = 0x1000) {
    private val bytes = mutableListOf<Byte>()
    val next: Long get() = start + bytes.size

    fun put(type: DataType, vararg values: Number): RecordBytes = apply {
        for (value in values) {
            val image = MemoryImage.of(0L to ByteArray(type.size))
            image.writeNumber(0, type, order, value.toDouble())
            bytes += image.read(0, type.size).toList()
        }
    }

    fun pad(count: Int) = apply { repeat(count) { bytes += 0xEE.toByte() } }
    fun text(text: String) = apply { bytes += text.encodeToByteArray().toList() }
    fun image(): MemoryImage = MemoryImage.of(start to bytes.toByteArray())
}

internal fun decoded(image: MemoryImage, obj: CalibrationObject, set: CalibrationSet? = null): CalibrationValues =
    when (val result = CalibrationCodec.decode(image, obj, set)) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

internal fun decodeError(image: MemoryImage, obj: CalibrationObject, set: CalibrationSet? = null): String =
    when (val result = CalibrationCodec.decode(image, obj, set)) {
        is TkResult.Success -> fail("${obj.name} was read")
        is TkResult.Failure -> result.error.message
    }

class CalibrationCodecTest {
    private val le = ByteOrder.LITTLE_ENDIAN

    private fun values(type: DataType, order: ValueOrder = ValueOrder.X_FIRST, pointer: DataType? = null) = RecordField.Values(type, order, pointer)

    private fun obj(
        kind: ObjectKind,
        vararg fields: RecordField,
        axes: List<CalibrationAxis> = emptyList(),
        conversion: Conversion = Conversion.Identical,
        order: ByteOrder = le,
        static: Boolean = false,
        alignments: Map<DataType, Int> = emptyMap(),
        dimensions: List<Int> = emptyList(),
        address: Long = 0x1000,
        lower: Double = Double.NEGATIVE_INFINITY,
        upper: Double = Double.POSITIVE_INFINITY,
        readOnly: Boolean = false,
        bitMask: Long? = null,
        encoding: String? = null,
        name: String = "Obj"
    ) = CalibrationObject(
        name, "", kind, address, Storage(fields.toList(), order, alignments, static), conversion, axes, dimensions,
        lowerLimit = lower, upperLimit = upper, readOnly = readOnly, bitMask = bitMask, encoding = encoding
    )

    private fun axis(source: AxisSource = AxisSource.Record, max: Int = 8, conversion: Conversion = Conversion.Identical, fixedCount: Int? = null, difference: Boolean = false) =
        CalibrationAxis(source, conversion, max, fixedCount = fixedCount, difference = difference)

    private fun <T> ok(result: TkResult<T, EncodeError>): T = when (result) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail(result.error.message)
    }

    private fun failure(result: TkResult<*, EncodeError>): EncodeError = when (result) {
        is TkResult.Success -> fail("It was written")
        is TkResult.Failure -> result.error
    }

    // ------------------------------------------------ Values and types

    @Test
    fun valueOfEveryTypeAndByteOrder() {
        for (order in ByteOrder.entries) {
            for (type in DataType.entries) {
                val sample = if (type.signed) -3.5 else 200.0
                val image = RecordBytes(order).put(type, sample).image()
                val values = decoded(image, obj(ObjectKind.VALUE, values(type), order = order))
                assertEquals(type.stored(sample), values.raw.single(), "$type $order")
                assertEquals(0, values.dimensions.size)
            }
        }
    }

    @Test
    fun valueThroughAConversion() {
        val image = RecordBytes().put(DataType.UBYTE, 100).image()
        val values = decoded(image, obj(ObjectKind.VALUE, values(DataType.UBYTE), conversion = Conversion.Linear(0.75, -48.0)))
        assertEquals(27.0, values.physical.single())
        assertEquals(listOf(AddressRange(0x1000, 0x1001)), values.ranges)
    }

    @Test
    fun bitMaskReadsAndWritesOnlyItsBits() {
        val image = RecordBytes().put(DataType.UBYTE, 0b1010_0101).image()
        val obj = obj(ObjectKind.VALUE, values(DataType.UBYTE), bitMask = 0b0011_0000)
        val values = decoded(image, obj)
        assertEquals(2.0, values.raw.single())
        ok(CalibrationCodec.write(image, values, 0, 1.0))
        assertEquals(0b1001_0101, image.byteAt(0x1000))
        assertEquals(EncodeError.OUT_OF_RANGE, failure(CalibrationCodec.write(image, values, 0, 4.0)), "two bits hold 0 to 3")
    }

    @Test
    fun valueBlockInBothOrders() {
        // 3 x 2, value (x, y) = 10 y + x
        val rowDir = RecordBytes().put(DataType.UBYTE, 0, 1, 2, 10, 11, 12).image()
        val columnDir = RecordBytes().put(DataType.UBYTE, 0, 10, 1, 11, 2, 12).image()
        for ((image, order) in listOf(rowDir to ValueOrder.X_FIRST, columnDir to ValueOrder.Y_FIRST)) {
            val values = decoded(image, obj(ObjectKind.VAL_BLK, values(DataType.UBYTE, order), dimensions = listOf(3, 2)))
            assertEquals(listOf(3, 2), values.dimensions.toList())
            assertEquals(12.0, values[2, 1], "$order")
            assertEquals(10.0, values[0, 1], "$order")
            assertEquals(listOf(0.0, 1.0, 2.0, 10.0, 11.0, 12.0), values.physical.toList(), "x first, whatever the storage")
        }
    }

    @Test
    fun asciiInUtf8AndUtf16() {
        val utf8 = RecordBytes().text("ECU-42").put(DataType.UBYTE, 0, 0).image()
        val text = decoded(utf8, obj(ObjectKind.ASCII, values(DataType.UBYTE), dimensions = listOf(8)))
        assertEquals("ECU-42", text.text)
        assertEquals(TkResult.Success(Unit), CalibrationCodec.writeAscii(utf8, text, "Tuned"))
        assertEquals("Tuned", decoded(utf8, obj(ObjectKind.ASCII, values(DataType.UBYTE), dimensions = listOf(8))).text)
        assertEquals(EncodeError.TOO_LONG, failure(CalibrationCodec.writeAscii(utf8, text, "Far too long")))

        val utf16 = RecordBytes(ByteOrder.BIG_ENDIAN).put(DataType.UWORD, 'H'.code, 'é'.code, 0).image()
        val wide = obj(ObjectKind.ASCII, values(DataType.UWORD), order = ByteOrder.BIG_ENDIAN, dimensions = listOf(3), encoding = "UTF16")
        assertEquals("Hé", decoded(utf16, wide).text)
    }

    // ------------------------------------------------ Curves and maps

    @Test
    fun curveWithItsAxisInTheRecord() {
        val image = RecordBytes()
            .put(DataType.UBYTE, 3)
            .put(DataType.UBYTE, 10, 20, 30)
            .put(DataType.UWORD, 100, 200, 300) // at 0x1004, already on a WORD boundary
            .image()
        val curve = obj(
            ObjectKind.CURVE,
            RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisPoints(0, DataType.UBYTE), values(DataType.UWORD),
            axes = listOf(axis(conversion = Conversion.Linear(10.0, 0.0)))
        )
        val values = decoded(image, curve)
        assertEquals(listOf(100.0, 200.0, 300.0), values.physical.toList())
        assertEquals(listOf(100.0, 200.0, 300.0), values.axes.single().physical.toList(), "axis points through their conversion")
        assertEquals(listOf(AddressRange(0x1000, 0x100A)), values.ranges)
    }

    @Test
    fun alignmentMovesFieldsToTheirBoundary() {
        // A UBYTE count, then UWORD points: without alignment they'd start at 0x1001
        val image = RecordBytes().put(DataType.UBYTE, 2).pad(1).put(DataType.UWORD, 5, 6).put(DataType.UBYTE, 7, 8).image()
        val curve = obj(
            ObjectKind.CURVE,
            RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisPoints(0, DataType.UWORD), values(DataType.UBYTE),
            axes = listOf(axis())
        )
        assertEquals(listOf(5.0, 6.0), decoded(image, curve).axes.single().raw.toList())
        // ALIGNMENT_WORD 1 packs them
        val packed = RecordBytes().put(DataType.UBYTE, 2).put(DataType.UWORD, 5, 6).put(DataType.UBYTE, 7, 8).image()
        val values = decoded(packed, obj(ObjectKind.CURVE, *curve.storage.fields.toTypedArray(), axes = curve.axes, alignments = mapOf(DataType.UWORD to 1)))
        assertEquals(listOf(7.0, 8.0), values.raw.toList())
    }

    @Test
    fun mapStoredColumnDirection() {
        // 2 x 3, stored y first: (0,0) (0,1) (0,2) (1,0) …, value (x, y) = 100 + 10 x + y
        val image = RecordBytes()
            .put(DataType.UBYTE, 2, 3)
            .put(DataType.UBYTE, 10, 20)
            .put(DataType.UBYTE, 1, 2, 3)
            .pad(1)
            .put(DataType.UWORD, 100, 101, 102, 110, 111, 112)
            .image()
        val map = obj(
            ObjectKind.MAP,
            RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisCount(1, DataType.UBYTE),
            RecordField.AxisPoints(0, DataType.UBYTE), RecordField.AxisPoints(1, DataType.UBYTE),
            values(DataType.UWORD, ValueOrder.Y_FIRST),
            axes = listOf(axis(), axis())
        )
        val values = decoded(image, map)
        assertEquals(listOf(2, 3), values.dimensions.toList())
        assertEquals(112.0, values[1, 2])
        assertEquals(101.0, values[0, 1])
        assertEquals(listOf(10.0, 20.0), values.axes[0].raw.toList())
        assertEquals(listOf(1.0, 2.0, 3.0), values.axes[1].raw.toList())
    }

    @Test
    fun staticRecordKeepsRoomForTheMaximum() {
        // 2 of at most 4 points; the values keep room for 4 x 3 and are stored y first with the maximum's stride
        val bytes = RecordBytes().put(DataType.UBYTE, 2).put(DataType.UBYTE, 1, 2, 0, 0)
        val valuesStart = bytes.next
        for (x in 0 until 4) for (y in 0 until 3) bytes.put(DataType.UBYTE, if (x < 2) 10 * x + y else 0xFF)
        val image = bytes.image()
        val map = obj(
            ObjectKind.MAP,
            RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisPoints(0, DataType.UBYTE),
            values(DataType.UBYTE, ValueOrder.Y_FIRST),
            axes = listOf(axis(max = 4), axis(AxisSource.Fixed(listOf(0.0, 1.0, 2.0)), max = 3)),
            static = true
        )
        val values = decoded(image, map)
        assertEquals(listOf(2, 3), values.dimensions.toList())
        assertEquals(12.0, values[1, 2])
        assertEquals(listOf(AddressRange(0x1000, valuesStart + 12)), values.ranges)
    }

    @Test
    fun decreasingAxisPointsAreShownIncreasing() {
        val image = RecordBytes().put(DataType.UBYTE, 30, 20, 10).put(DataType.UBYTE, 3, 2, 1).image()
        val curve = obj(ObjectKind.CURVE, RecordField.AxisPoints(0, DataType.UBYTE, decreasing = true), values(DataType.UBYTE), axes = listOf(axis(fixedCount = 3)))
        val values = decoded(image, curve)
        assertEquals(listOf(10.0, 20.0, 30.0), values.axes.single().raw.toList())
        ok(CalibrationCodec.writeAxis(image, values, 0, 0, 5.0))
        assertEquals(5, image.byteAt(0x1002), "point 0 is stored last")
    }

    @Test
    fun differenceAxisAddsUpAndWritesBothDifferences() {
        val image = RecordBytes().put(DataType.UBYTE, 10, 5, 5).put(DataType.UBYTE, 0, 0, 0).image()
        val curve = obj(ObjectKind.CURVE, RecordField.AxisPoints(0, DataType.UBYTE), values(DataType.UBYTE), axes = listOf(axis(fixedCount = 3, difference = true)))
        val values = decoded(image, curve)
        assertEquals(listOf(10.0, 15.0, 20.0), values.axes.single().raw.toList())
        ok(CalibrationCodec.writeAxis(image, values, 0, 1, 12.0))
        assertEquals(listOf(10.0, 12.0, 20.0), decoded(image, curve).axes.single().raw.toList(), "point 2 stays where it was")
        assertEquals(listOf(10, 2, 8), (0x1000L..0x1002L).map { image.byteAt(it) })
    }

    @Test
    fun axisFromOffsetAndDistance() {
        val image = RecordBytes().put(DataType.UBYTE, 3).put(DataType.UWORD, 1000, 500).put(DataType.UBYTE, 7, 8, 9).image()
        val curve = obj(
            ObjectKind.CURVE,
            RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisOffset(0, DataType.UWORD), RecordField.AxisDistance(0, DataType.UWORD), values(DataType.UBYTE),
            axes = listOf(axis()), alignments = mapOf(DataType.UWORD to 1)
        )
        val values = decoded(image, curve)
        assertEquals(listOf(1000.0, 1500.0, 2000.0), values.axes.single().raw.toList())
        assertEquals(EncodeError.COMPUTED_AXIS, failure(CalibrationCodec.writeAxis(image, values, 0, 0, 1.0)))
    }

    @Test
    fun axisFromOffsetAndShift() {
        val image = RecordBytes().put(DataType.UBYTE, 4, 3).put(DataType.UBYTE, 1, 2).image()
        val curve = obj(
            ObjectKind.CURVE,
            RecordField.AxisOffset(0, DataType.UBYTE), RecordField.AxisShift(0, DataType.UBYTE), values(DataType.UBYTE),
            axes = listOf(axis(fixedCount = 2))
        )
        assertEquals(listOf(4.0, 12.0), decoded(image, curve).axes.single().raw.toList())
    }

    @Test
    fun valuesBehindAPointer() {
        // The record holds a ULONG pointer to the values at 0x1010
        val image = RecordBytes().put(DataType.ULONG, 0x1010).pad(12).put(DataType.UWORD, 7, 8).image()
        val block = obj(ObjectKind.VAL_BLK, values(DataType.UWORD, pointer = DataType.ULONG), dimensions = listOf(2))
        val values = decoded(image, block)
        assertEquals(listOf(7.0, 8.0), values.raw.toList())
        assertEquals(listOf(AddressRange(0x1000, 0x1004), AddressRange(0x1010, 0x1014)), values.ranges)
    }

    // ------------------------------------------------ Axes from elsewhere

    @Test
    fun sharedAxisIsReadFromItsOwnObjectAndWritesThere() {
        val image = RecordBytes()
            .put(DataType.UBYTE, 3, 10, 20, 30) // the shared axis at 0x1000
            .put(DataType.UBYTE, 1, 2, 3) // the curve's values at 0x1004
            .image()
        val shared = obj(
            ObjectKind.AXIS, RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisPoints(0, DataType.UBYTE),
            axes = listOf(axis(conversion = Conversion.Linear(2.0, 0.0))), conversion = Conversion.Linear(2.0, 0.0), name = "Speed"
        )
        val curve = obj(ObjectKind.CURVE, values(DataType.UBYTE), axes = listOf(axis(AxisSource.Shared("Speed"))), address = 0x1004)
        val set = CalibrationSet(listOf(shared, curve))
        val axis = decoded(image, shared)
        assertEquals(listOf(20.0, 40.0, 60.0), axis.physical.toList(), "an AXIS object's values are its points")
        val values = decoded(image, curve, set)
        assertEquals(listOf(20.0, 40.0, 60.0), values.axes.single().physical.toList())
        assertEquals("Speed", values.axes.single().sharedWith)
        ok(CalibrationCodec.writeAxis(image, values, 0, 2, 70.0))
        assertEquals(35, image.byteAt(0x1003))
        assertTrue("isn't defined" in decodeError(image, curve, CalibrationSet(listOf(curve))))
        val plain = obj(ObjectKind.VALUE, values(DataType.UBYTE), name = "Plain")
        val wrong = obj(ObjectKind.CURVE, values(DataType.UBYTE), axes = listOf(axis(AxisSource.Shared("Plain"))))
        assertTrue("not an axis" in decodeError(image, wrong, CalibrationSet(listOf(plain, wrong))))
    }

    @Test
    fun rescaleAxisIsInterpolatedOverItsPairs() {
        // Pairs (axis value, virtual index): (0, 0) (100, 2) (400, 4)
        val image = RecordBytes().put(DataType.UBYTE, 3).pad(1).put(DataType.UWORD, 0, 0, 100, 2, 400, 4).image()
        val rescale = obj(
            ObjectKind.AXIS, RecordField.RescaleCount(0, DataType.UBYTE), RecordField.Rescale(0, DataType.UWORD, maxPairs = 8),
            axes = listOf(axis()), name = "Rescale"
        )
        val map = obj(ObjectKind.CURVE, values(DataType.UBYTE), axes = listOf(axis(AxisSource.Rescaled("Rescale"), max = 5)), address = 0x2000)
        val curveImage = MemoryImage.of(0x1000L to image.read(0x1000, 14), 0x2000L to byteArrayOf(1, 2, 3, 4, 5))
        val values = decoded(curveImage, map, CalibrationSet(listOf(rescale, map)))
        assertEquals(listOf(0.0, 50.0, 100.0, 250.0, 400.0), values.axes.single().raw.toList())
        val own = decoded(curveImage, rescale)
        assertEquals(listOf(0.0, 100.0, 400.0), own.raw.toList())
        assertEquals(listOf(0.0, 2.0, 4.0), own.rescaleIndexes?.toList())
    }

    @Test
    fun fixedAndCurveAxes() {
        val image = RecordBytes().put(DataType.UBYTE, 1, 2, 3).image()
        val fixed = obj(ObjectKind.CURVE, values(DataType.UBYTE), axes = listOf(axis(AxisSource.Fixed(listOf(10.0, 20.0, 30.0)), conversion = Conversion.Linear(0.1, 0.0))))
        assertEquals(listOf(1.0, 2.0, 3.0), decoded(image, fixed).axes.single().physical.toList())
        val curveAxis = obj(ObjectKind.CURVE, values(DataType.UBYTE), axes = listOf(axis(AxisSource.Curve("Lookup"), max = 3)))
        assertEquals(listOf(0.0, 1.0, 2.0), decoded(image, curveAxis).axes.single().raw.toList())
    }

    // ------------------------------------------------ Cubes

    /** Fills a cube stored in [order] so each value names its own position, then checks every position reads back */
    private fun checkCube(kind: ObjectKind, sizes: List<Int>, order: ValueOrder) {
        val bytes = RecordBytes()
        // Storage walk: the inner two in the order's direction, the rest outermost
        val inner = if (order == ValueOrder.Y_FIRST && sizes.size >= 2) listOf(1, 0) else listOf(0, 1).take(sizes.size)
        val walk = inner + (2 until sizes.size)
        val position = IntArray(sizes.size)
        fun code() = position.foldIndexed(0) { i, acc, p -> acc + p * listOf(1, 3, 9, 27, 81)[i] }
        fun fill(level: Int) {
            if (level < 0) {
                bytes.put(DataType.UBYTE, code())
                return
            }
            val d = walk[level]
            for (p in 0 until sizes[d]) {
                position[d] = p
                fill(level - 1)
            }
        }
        fill(walk.size - 1)
        val cube = obj(kind, values(DataType.UBYTE, order), axes = sizes.map { axis(AxisSource.Fixed(List(it) { i -> i.toDouble() })) })
        val values = decoded(bytes.image(), cube)
        assertEquals(sizes, values.dimensions.toList())
        val at = IntArray(sizes.size)
        for (i in 0 until values.size) {
            var rest = i
            for (d in sizes.indices) {
                at[d] = rest % sizes[d]
                rest /= sizes[d]
            }
            assertEquals(at.foldIndexed(0) { d, acc, p -> acc + p * listOf(1, 3, 9, 27, 81)[d] }.toDouble(), values.physical[values.index(*at)], "$kind $order ${at.toList()}")
        }
    }

    @Test
    fun cuboidCube4AndCube5InBothOrders() {
        for (order in ValueOrder.entries) {
            checkCube(ObjectKind.CUBOID, listOf(2, 3, 2), order)
            checkCube(ObjectKind.CUBE_4, listOf(2, 2, 3, 2), order)
            checkCube(ObjectKind.CUBE_5, listOf(3, 2, 2, 1, 2), order)
        }
    }

    // ------------------------------------------------ Problems

    @Test
    fun badCountsAndMissingBytesAreErrors() {
        val curve = obj(ObjectKind.CURVE, RecordField.AxisCount(0, DataType.UBYTE), RecordField.AxisPoints(0, DataType.UBYTE), values(DataType.UBYTE), axes = listOf(axis(max = 4)))
        assertTrue("has 9 points" in decodeError(RecordBytes().put(DataType.UBYTE, 9).image(), curve))
        assertTrue("has 0 points" in decodeError(RecordBytes().put(DataType.UBYTE, 0).image(), curve))
        assertTrue("isn't in the image" in decodeError(RecordBytes().put(DataType.UBYTE, 3, 1, 2, 3).image(), curve))
        assertTrue("needs 2 axes" in decodeError(MemoryImage.empty(), obj(ObjectKind.MAP, values(DataType.UBYTE), axes = listOf(axis()))))
        assertTrue("axis Y" in decodeError(MemoryImage.empty(), obj(ObjectKind.CURVE, RecordField.AxisCount(1, DataType.UBYTE), values(DataType.UBYTE), axes = listOf(axis()))))
        val huge = obj(ObjectKind.VAL_BLK, values(DataType.UBYTE), dimensions = listOf(100_000, 100_000))
        assertTrue("more than" in decodeError(MemoryImage.empty(), huge))
        // A product that would overflow a Long and wrap round to something small is still refused
        val overflowing = obj(ObjectKind.VAL_BLK, values(DataType.UBYTE), dimensions = List(3) { Int.MAX_VALUE })
        assertTrue("more than" in decodeError(MemoryImage.empty(), overflowing))
    }

    @Test
    fun countAfterWhatItSizesFallsBackToTheMaximumWithAWarning() {
        val image = RecordBytes().put(DataType.UBYTE, 1, 2, 2).image()
        val curve = obj(ObjectKind.CURVE, RecordField.AxisPoints(0, DataType.UBYTE), RecordField.AxisCount(0, DataType.UBYTE), values(DataType.UBYTE), axes = listOf(axis(max = 2)))
        val values = decoded(RecordBytes().put(DataType.UBYTE, 1, 2, 2, 7, 8).image(), curve)
        assertEquals(listOf(7.0, 8.0), values.raw.toList())
        assertTrue(values.warnings.single().message.contains("maximum 2"))
        assertTrue("isn't in the image" in decodeError(image, curve))
    }

    // ------------------------------------------------ Writing

    @Test
    fun writingConvertsRoundsAndChecks() {
        val image = RecordBytes().put(DataType.UBYTE, 100).image()
        val obj = obj(ObjectKind.VALUE, values(DataType.UBYTE), conversion = Conversion.Linear(0.75, -48.0), lower = -48.0, upper = 100.0)
        val values = decoded(image, obj)
        assertEquals(30.0, ok(CalibrationCodec.write(image, values, 0, 30.1)), "104 raw is 30.0, the closest to 30.1")
        assertEquals(104, image.byteAt(0x1000))
        assertEquals(EncodeError.OUT_OF_LIMITS, failure(CalibrationCodec.write(image, values, 0, 120.0)))
        assertEquals(EncodeError.OUT_OF_RANGE, failure(CalibrationCodec.write(image, values, 0, 160.0, checkLimits = false)), "255 raw is 143.25")
        assertEquals(EncodeError.NOT_A_NUMBER, failure(CalibrationCodec.write(image, values, 0, Double.NaN)))
        assertEquals(EncodeError.NO_SUCH_VALUE, failure(CalibrationCodec.write(image, values, 3, 1.0)))
        assertEquals(1.5, ok(CalibrationCodec.writeRaw(image, values, 0, 66.0)))
        assertEquals(1.5, ok(CalibrationCodec.writeRaw(image, values, 0, 66.0)), "writing what's already there is fine")
        val readOnly = decoded(image, obj(ObjectKind.VALUE, values(DataType.UBYTE), readOnly = true))
        assertEquals(EncodeError.READ_ONLY, failure(CalibrationCodec.write(image, readOnly, 0, 1.0)))
    }

    @Test
    fun writingUndoesAsOneStep() {
        val image = RecordBytes().put(DataType.UWORD, 1, 2).image()
        val block = decoded(image, obj(ObjectKind.VAL_BLK, values(DataType.UWORD), dimensions = listOf(2)))
        ok(CalibrationCodec.write(image, block, 1, 500.0))
        assertEquals(listOf(1.0, 500.0), decoded(image, block.obj).raw.toList())
        image.undo()
        assertEquals(listOf(1.0, 2.0), decoded(image, block.obj).raw.toList())
    }

    @Test
    fun verbalValuesAreWrittenByText() {
        val image = RecordBytes().put(DataType.UBYTE, 0).image()
        val values = decoded(image, obj(ObjectKind.VALUE, values(DataType.UBYTE), conversion = Conversion.Verbal(listOf(0.0 to "Off", 1.0 to "On"))))
        assertEquals("Off", values.textAt(0))
        ok(CalibrationCodec.writeText(image, values, 0, "On"))
        assertEquals(1, image.byteAt(0x1000))
        assertEquals(EncodeError.UNKNOWN_TEXT, failure(CalibrationCodec.writeText(image, values, 0, "Maybe")))
    }

    @Test
    fun formulaWithoutAnInverseIsSearched() {
        val formula = (Expression.parse("X1 * X1 / 100") as TkResult.Success).data
        val image = RecordBytes().put(DataType.UBYTE, 50).image()
        val values = decoded(image, obj(ObjectKind.VALUE, values(DataType.UBYTE), conversion = Conversion.Formula(formula)))
        assertEquals(25.0, values.physical.single())
        assertEquals(36.0, ok(CalibrationCodec.write(image, values, 0, 36.0)))
        assertEquals(60, image.byteAt(0x1000))
        assertEquals(EncodeError.NO_RAW_VALUE, failure(CalibrationCodec.write(image, values, 0, 1000.0)), "255² / 100 is the most")
    }

    @Test
    fun floatsAreWrittenAsFloats() {
        val image = RecordBytes(ByteOrder.BIG_ENDIAN).put(DataType.FLOAT32_IEEE, 1.5).image()
        val values = decoded(image, obj(ObjectKind.VALUE, values(DataType.FLOAT32_IEEE), order = ByteOrder.BIG_ENDIAN))
        assertEquals(0.1f.toDouble(), ok(CalibrationCodec.write(image, values, 0, 0.1)))
        assertEquals(0.1f, image.readFloat32(0x1000, ByteOrder.BIG_ENDIAN))
    }
}
