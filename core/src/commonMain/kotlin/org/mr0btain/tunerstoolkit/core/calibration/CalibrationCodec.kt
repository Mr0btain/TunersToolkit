package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.EmptyResult
import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.merged
import org.mr0btain.tunerstoolkit.core.image.toHexAddress

/** Why an object couldn't be read from an image */
class DecodeError(override val message: String) : Error

/** Why a value couldn't be written */
enum class EncodeError(override val message: String) : Error {
    READ_ONLY("This object is read only"),
    NOT_IN_IMAGE("The image doesn't hold this value's bytes"),
    OUT_OF_LIMITS("The value is outside the object's limits"),
    OUT_OF_RANGE("The value doesn't fit the type it's stored as"),
    NO_RAW_VALUE("No stored value gives this physical value"),
    NOT_A_NUMBER("That isn't a number"),
    UNKNOWN_TEXT("That isn't one of the object's texts"),
    COMPUTED_AXIS("This axis isn't stored as points, so they can't be changed one by one"),
    TOO_LONG("The text is longer than the space for it"),
    NO_SUCH_VALUE("There's no value at that position")
}

/** What a part of an object's memory holds */
enum class PartRole {
    /** Its values (for an AXIS object, its points) */
    VALUES,

    /** An axis's points stored in its own record */
    AXIS_POINTS,

    /** Everything else in the record: counts, offsets, pointers and reserved space */
    RECORD
}

/**
 * One stretch of an object's own memory and what it holds. [axis] is the axis for [PartRole.AXIS_POINTS], -1 otherwise
 */
class ObjectPart(val range: AddressRange, val role: PartRole, val axis: Int = -1) {
    override fun toString(): String = "$role${if (axis >= 0) " $axis" else ""} $range"
}

/** Where a run of numbers is stored, so they can be written back */
class Slots internal constructor(
    val addresses: LongArray,
    val type: DataType,
    val byteOrder: ByteOrder,
    val bitMask: Long? = null,
    /** Stored as differences from the point before */
    val difference: Boolean = false
)

/**
 * An axis's points as read
 * @property conversion The conversion the points use (a shared axis's own)
 * @property slots Where they're stored; null when they're computed (fixed, offset and step, rescaled, curve indices)
 * @property sharedWith The AXIS object they're stored in, when it's shared
 */
class AxisValues(
    val axis: CalibrationAxis,
    val conversion: Conversion,
    val raw: DoubleArray,
    val physical: DoubleArray,
    val slots: Slots?,
    val sharedWith: String? = null
) {
    val count: Int get() = raw.size
    fun text(index: Int): String? = conversion.text(raw[index])
}

/**
 * An object's contents read from an image. Values are in x first order: value (x, y, z) is at
 * x + xCount * (y + yCount * z), whatever order they're stored in
 * @property address Where the record was read from (a variant's, when one was picked)
 * @property dimensions How many values along each axis; empty for a single value
 * @property text The text of an ASCII object
 * @property rescaleIndexes For a rescale AXIS object, the virtual index of each pair (its values are the axis values)
 * @property ranges The memory it was read from, merged: its own record, anything pointers led to, and shared axes
 * @property parts Its own memory (not shared axes'), part by part with what each holds, for highlighting
 * @property warnings Things that looked off but didn't stop it being read
 */
class CalibrationValues(
    val obj: CalibrationObject,
    val address: Long,
    val dimensions: IntArray,
    val raw: DoubleArray,
    val physical: DoubleArray,
    val axes: List<AxisValues>,
    val slots: Slots?,
    val text: String? = null,
    val rescaleIndexes: DoubleArray? = null,
    val ranges: List<AddressRange> = emptyList(),
    val parts: List<ObjectPart> = emptyList(),
    val warnings: List<Diagnostic> = emptyList()
) {
    val size: Int get() = raw.size

    /** The index in [raw] and [physical] of the value at [at] (x first) */
    fun index(vararg at: Int): Int {
        var index = 0
        var stride = 1
        for (i in dimensions.indices) {
            val position = at.getOrElse(i) { 0 }
            require(position in 0 until dimensions[i]) { "Position $position is outside 0 until ${dimensions[i]}" }
            index += position * stride
            stride *= dimensions[i]
        }
        return index
    }

    operator fun get(vararg at: Int): Double = physical[index(*at)]

    /** The text shown for a value, for verbal conversions */
    fun textAt(index: Int): String? = obj.conversion.text(raw[index])
}

/**
 * Reads calibration objects from a [MemoryImage] and writes edits back. Never throws: anything that can't be read is
 * a [DecodeError] naming the object and why, and writes that can't happen are an [EncodeError]
 */
object CalibrationCodec {
    /**
     * @param set Where shared and rescale axes are looked up
     * @param variant Which of [CalibrationObject.variantAddresses] to read; null for the main address
     */
    fun decode(image: MemoryImage, obj: CalibrationObject, set: CalibrationSet? = null, variant: Int? = null): TkResult<CalibrationValues, DecodeError> {
        val address = if (variant == null) obj.address else obj.variantAddresses.getOrNull(variant)
            ?: return TkResult.Failure(DecodeError("${obj.name} has no variant $variant"))
        return try {
            TkResult.Success(RecordReader(image, obj, set, address).read())
        } catch (e: DecodeFailure) {
            TkResult.Failure(DecodeError("${obj.name}: ${e.message}"))
        }
    }

    /**
     * Writes [physical] as the value at [index], converted, rounded to its type and checked against its limits
     * (and the type's range). [checkLimits] false checks only the extended limits, when there are any
     * @return The physical value actually stored, after rounding
     */
    fun write(image: MemoryImage, values: CalibrationValues, index: Int, physical: Double, checkLimits: Boolean = true): TkResult<Double, EncodeError> {
        val obj = values.obj
        val slots = values.slots ?: return TkResult.Failure(EncodeError.READ_ONLY)
        if (obj.readOnly) return TkResult.Failure(EncodeError.READ_ONLY)
        if (index !in 0 until values.size) return TkResult.Failure(EncodeError.NO_SUCH_VALUE)
        if (physical.isNaN()) return TkResult.Failure(EncodeError.NOT_A_NUMBER)
        if (!withinLimits(physical, obj.lowerLimit, obj.upperLimit, obj.extendedLimits, checkLimits)) return TkResult.Failure(EncodeError.OUT_OF_LIMITS)
        val raw = obj.conversion.toRaw(physical, slots.rangeOfValues())
        return writeRawAt(image, slots, values.raw, index, raw, obj.conversion)
    }

    /** Writes a raw value as it is (rounded to its type) */
    fun writeRaw(image: MemoryImage, values: CalibrationValues, index: Int, raw: Double): TkResult<Double, EncodeError> {
        val slots = values.slots ?: return TkResult.Failure(EncodeError.READ_ONLY)
        if (values.obj.readOnly) return TkResult.Failure(EncodeError.READ_ONLY)
        if (index !in 0 until values.size) return TkResult.Failure(EncodeError.NO_SUCH_VALUE)
        return writeRawAt(image, slots, values.raw, index, raw, values.obj.conversion)
    }

    /** Writes the raw value a verbal conversion shows as [text] */
    fun writeText(image: MemoryImage, values: CalibrationValues, index: Int, text: String): TkResult<Double, EncodeError> {
        val raw = values.obj.conversion.rawForText(text) ?: return TkResult.Failure(EncodeError.UNKNOWN_TEXT)
        return writeRaw(image, values, index, raw)
    }

    /**
     * Writes axis point [index] of axis [axis]. A shared axis's points are in its own object, so every object using
     * it changes
     * @return The physical value actually stored
     */
    fun writeAxis(image: MemoryImage, values: CalibrationValues, axis: Int, index: Int, physical: Double, checkLimits: Boolean = true): TkResult<Double, EncodeError> {
        val axisValues = values.axes.getOrNull(axis) ?: return TkResult.Failure(EncodeError.NO_SUCH_VALUE)
        val slots = axisValues.slots ?: return TkResult.Failure(EncodeError.COMPUTED_AXIS)
        if (values.obj.readOnly) return TkResult.Failure(EncodeError.READ_ONLY)
        if (index !in 0 until axisValues.count) return TkResult.Failure(EncodeError.NO_SUCH_VALUE)
        if (physical.isNaN()) return TkResult.Failure(EncodeError.NOT_A_NUMBER)
        if (!withinLimits(physical, axisValues.axis.lowerLimit, axisValues.axis.upperLimit, null, checkLimits)) return TkResult.Failure(EncodeError.OUT_OF_LIMITS)
        val raw = axisValues.conversion.toRaw(physical, slots.rangeOfValues())
        return writeRawAt(image, slots, axisValues.raw, index, raw, axisValues.conversion)
    }

    /** Writes an ASCII object's text, padded with zeros to its length */
    fun writeAscii(image: MemoryImage, values: CalibrationValues, text: String): EmptyResult<EncodeError> {
        val obj = values.obj
        val slots = values.slots ?: return TkResult.Failure(EncodeError.READ_ONLY)
        if (obj.readOnly) return TkResult.Failure(EncodeError.READ_ONLY)
        val length = values.size * slots.type.size
        val bytes = TextCoding.encode(text, obj.encoding, slots.byteOrder)
        if (bytes.size > length) return TkResult.Failure(EncodeError.TOO_LONG)
        val written = image.write(slots.addresses.first(), bytes.copyOf(length))
        return if (written) TkResult.Success(Unit) else TkResult.Failure(EncodeError.NOT_IN_IMAGE)
    }

    private fun withinLimits(value: Double, lower: Double, upper: Double, extended: ClosedFloatingPointRange<Double>?, check: Boolean): Boolean {
        if (check) return value >= lower && value <= upper
        return extended == null || value in extended
    }

    /** The raw values a slot can hold: the type's range, or the mask's when only some bits are its */
    private fun Slots.rangeOfValues(): ClosedFloatingPointRange<Double> {
        val mask = bitMask ?: return type.range
        return 0.0..(mask ushr mask.countTrailingZeroBits()).toDouble()
    }

    private fun writeRawAt(image: MemoryImage, slots: Slots, current: DoubleArray, index: Int, raw: Double, conversion: Conversion): TkResult<Double, EncodeError> {
        if (raw.isNaN() || raw.isInfinite()) return TkResult.Failure(EncodeError.NO_RAW_VALUE)
        val stored = slots.type.stored(raw)
        if (stored !in slots.rangeOfValues()) return TkResult.Failure(EncodeError.OUT_OF_RANGE)
        if (slots.difference) {
            // Point i is stored as point i minus point i - 1, so the next point's difference changes too
            val points = current.copyOf()
            points[index] = stored
            val changes = listOf(index, index + 1).filter { it < points.size }.map { i -> i to (if (i == 0) points[0] else points[i] - points[i - 1]) }
            if (changes.any { slots.type.stored(it.second) !in slots.rangeOfValues() }) return TkResult.Failure(EncodeError.OUT_OF_RANGE)
            val written = image.edit { changes.all { (i, value) -> writeSlot(this, slots, i, value) } }
            if (!written) return TkResult.Failure(EncodeError.NOT_IN_IMAGE)
        } else if (!writeSlot(image, slots, index, stored)) {
            return TkResult.Failure(EncodeError.NOT_IN_IMAGE)
        }
        return TkResult.Success(conversion.toPhysical(stored))
    }

    private fun writeSlot(image: MemoryImage, slots: Slots, index: Int, raw: Double): Boolean {
        val address = slots.addresses[index]
        val mask = slots.bitMask ?: return image.writeNumber(address, slots.type, slots.byteOrder, raw)
        if (!image.contains(address, slots.type.size)) return false
        val current = image.readUnsigned(address, slots.type.size, slots.byteOrder).toLong()
        val bits = (current and mask.inv()) or ((raw.toLong() shl mask.countTrailingZeroBits()) and mask)
        return image.writeUnsigned(address, slots.type.size, bits.toULong(), slots.byteOrder)
    }
}

/** Reads text in A2L's encodings: UTF8 (and plain ASCII), UTF16 and UTF32, stopping at the first zero */
internal object TextCoding {
    fun decode(bytes: ByteArray, encoding: String?, order: ByteOrder): String {
        val unit = unitSize(encoding)
        if (unit == 1) {
            val end = bytes.indexOf(0).let { if (it < 0) bytes.size else it }
            return bytes.copyOf(end).decodeToString()
        }
        val builder = StringBuilder()
        var i = 0
        while (i + unit <= bytes.size) {
            val code = order.decode(bytes, i, unit).toInt()
            if (code == 0) break
            if (unit == 2) builder.append(code.toChar()) else appendCodePoint(builder, code)
            i += unit
        }
        return builder.toString()
    }

    fun encode(text: String, encoding: String?, order: ByteOrder): ByteArray {
        val unit = unitSize(encoding)
        if (unit == 1) return text.encodeToByteArray()
        val codes = if (unit == 2) text.map { it.code } else codePoints(text)
        val bytes = ByteArray(codes.size * unit)
        codes.forEachIndexed { i, code -> order.encode(code.toULong(), unit).copyInto(bytes, i * unit) }
        return bytes
    }

    private fun unitSize(encoding: String?) = when (encoding?.uppercase()) {
        "UTF16" -> 2
        "UTF32" -> 4
        else -> 1
    }

    private fun appendCodePoint(builder: StringBuilder, code: Int) {
        if (code < 0x10000) {
            builder.append(code.toChar())
        } else {
            val shifted = code - 0x10000
            builder.append((0xD800 + (shifted shr 10)).toChar()).append((0xDC00 + (shifted and 0x3FF)).toChar())
        }
    }

    private fun codePoints(text: String): List<Int> {
        val codes = mutableListOf<Int>()
        var i = 0
        while (i < text.length) {
            val high = text[i]
            if (high.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                codes += 0x10000 + ((high.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00)
                i += 2
            } else {
                codes += high.code
                i++
            }
        }
        return codes
    }
}

private class DecodeFailure(message: String) : Exception(message)

/**
 * Walks one object's record field by field. Counts read early size the fields after them; a count that's only known
 * later (or not stored at all) falls back to the axis's maximum
 */
private class RecordReader(
    private val image: MemoryImage,
    private val obj: CalibrationObject,
    private val set: CalibrationSet?,
    private val start: Long
) {
    private val storage = obj.storage
    private val order = storage.byteOrder
    private val axisCount = if (obj.kind == ObjectKind.AXIS) 1 else obj.axes.size
    private val counts = IntArray(axisCount) { -1 }
    private val ranges = mutableListOf<AddressRange>()
    private val parts = mutableListOf<ObjectPart>()

    private fun own(range: AddressRange, role: PartRole, axis: Int = -1) {
        if (range.size <= 0) return
        ranges += range
        parts += ObjectPart(range, role, axis)
    }
    private val warnings = mutableListOf<Diagnostic>()

    private val pointRaw = arrayOfNulls<DoubleArray>(axisCount)
    private val pointSlots = arrayOfNulls<Slots>(axisCount)
    private val offsets = arrayOfNulls<Double>(axisCount)
    private val distances = arrayOfNulls<Double>(axisCount)
    private val rescaleCounts = IntArray(axisCount) { -1 }
    private var rescalePairs: List<Pair<Double, Double>>? = null
    private var rescaleSlots: Slots? = null
    private val external = arrayOfNulls<AxisValues>(axisCount)

    private var valueRaw: DoubleArray? = null
    private var valueSlots: Slots? = null
    private var valueDimensions = IntArray(0)

    /** Axes whose count is a field in the record, so using the maximum before it's read is worth a warning */
    private val countedAxes = storage.fields.mapNotNull { (it as? RecordField.AxisCount)?.axis }.toSet()

    fun read(): CalibrationValues {
        checkShape()
        for (i in 0 until axisCount) prepareAxis(i)
        var cursor = start
        for (field in storage.fields) cursor = readField(field, cursor)
        val axes = (0 until axisCount).map { buildAxis(it) }
        return when (obj.kind) {
            ObjectKind.AXIS -> axisObject(axes.single())
            else -> {
                val raw = valueRaw ?: throw DecodeFailure("its record has no values")
                CalibrationValues(
                    obj, start, valueDimensions, raw, DoubleArray(raw.size) { obj.conversion.toPhysical(raw[it]) }, axes, valueSlots,
                    text = if (obj.kind == ObjectKind.ASCII) asciiText() else null,
                    ranges = ranges.merged(), parts = parts.toList(), warnings = warnings
                )
            }
        }
    }

    // ------------------------------------------------ Before the record: axes from elsewhere

    /** The object has the axes its kind needs, and its record only names those */
    private fun checkShape() {
        if (obj.axes.size < maxOf(obj.kind.axisCount, axisCount)) {
            throw DecodeFailure("a ${obj.kind} needs ${obj.kind.axisCount} axes, it has ${obj.axes.size}")
        }
        for (field in storage.fields) {
            val axis = when (field) {
                is RecordField.AxisCount -> field.axis
                is RecordField.AxisPoints -> field.axis
                is RecordField.Rescale -> field.axis
                is RecordField.RescaleCount -> field.axis
                is RecordField.AxisOffset -> field.axis
                is RecordField.AxisDistance -> field.axis
                is RecordField.AxisShift -> field.axis
                else -> continue
            }
            if (axis !in 0 until axisCount) throw DecodeFailure("its record has a field for axis ${axisName(axis)}, which it doesn't have")
        }
    }

    private fun axis(i: Int): CalibrationAxis = obj.axes.getOrNull(i) ?: throw DecodeFailure("it has no axis ${axisName(i)}")

    private fun prepareAxis(i: Int) {
        val axis = axis(i)
        axis.fixedCount?.let {
            if (it !in 1..MAX_VALUES) throw DecodeFailure("axis ${axisName(i)} has $it points")
            counts[i] = it
        }
        when (val source = axis.source) {
            is AxisSource.Shared -> {
                val shared = sharedAxis(source.name, i)
                counts[i] = shared.size
                external[i] = AxisValues(axis, shared.obj.conversion, shared.raw, shared.physical, shared.slots, shared.obj.name)
            }
            is AxisSource.Rescaled -> {
                val shared = sharedAxis(source.name, i)
                val indexes = shared.rescaleIndexes ?: throw DecodeFailure("axis ${axisName(i)}'s ${source.name} has no rescale pairs")
                val count = axis.fixedCount ?: axis.maxPoints.takeIf { it > 0 } ?: ((indexes.lastOrNull() ?: 0.0).toInt() + 1)
                if (count !in 1..MAX_VALUES) throw DecodeFailure("axis ${axisName(i)} has $count points")
                val raw = DoubleArray(count) { k -> interpolate(indexes, shared.raw, k.toDouble()) }
                counts[i] = count
                external[i] = AxisValues(axis, shared.obj.conversion, raw, DoubleArray(count) { shared.obj.conversion.toPhysical(raw[it]) }, null, shared.obj.name)
            }
            is AxisSource.Fixed -> {
                val raw = source.points.toDoubleArray()
                counts[i] = raw.size
                external[i] = AxisValues(axis, axis.conversion, raw, DoubleArray(raw.size) { axis.conversion.toPhysical(raw[it]) }, null)
            }
            is AxisSource.Curve -> {
                val count = axis.fixedCount ?: axis.maxPoints
                if (count !in 1..MAX_VALUES) throw DecodeFailure("axis ${axisName(i)} has $count points")
                val raw = DoubleArray(count) { it.toDouble() }
                counts[i] = count
                external[i] = AxisValues(axis, axis.conversion, raw, DoubleArray(count) { axis.conversion.toPhysical(raw[it]) }, null, source.name)
            }
            AxisSource.Record -> Unit
        }
    }

    private fun sharedAxis(name: String, i: Int): CalibrationValues {
        val shared = set?.get(name) ?: throw DecodeFailure("axis ${axisName(i)}'s $name isn't defined")
        if (shared.kind != ObjectKind.AXIS) throw DecodeFailure("axis ${axisName(i)}'s $name is a ${shared.kind}, not an axis")
        return when (val result = CalibrationCodec.decode(image, shared, null)) {
            is TkResult.Success -> result.data.also { ranges += it.ranges }
            is TkResult.Failure -> throw DecodeFailure("axis ${axisName(i)} couldn't be read: ${result.error.message}")
        }
    }

    // ------------------------------------------------ The record

    /** Reads [field] at [cursor] (aligned first), returns where the next field starts */
    private fun readField(field: RecordField, cursor: Long): Long {
        // A field held behind a pointer is aligned as the pointer, which is what's in the record
        val pointer = when (field) {
            is RecordField.Values -> field.pointer
            is RecordField.AxisPoints -> field.pointer
            is RecordField.Rescale -> field.pointer
            else -> null
        }
        val at = align(cursor, pointer ?: field.type)
        // Counts, offsets and the space skipped are part of the record too; data fields add their own ranges
        if (field !is RecordField.AxisPoints && field !is RecordField.Rescale && field !is RecordField.Values) {
            own(AddressRange.ofSize(at, field.type.size.toLong()), PartRole.RECORD)
        }
        return when (field) {
            is RecordField.AxisCount -> {
                val count = number(at, field.type, "the number of ${axisName(field.axis)} points").toLong()
                val max = maxOf(axis(field.axis).maxPoints, 0)
                if (count < 1 || (max > 0 && count > max) || count > MAX_VALUES) {
                    throw DecodeFailure("it says axis ${axisName(field.axis)} has $count points, but it can have 1 to $max")
                }
                counts[field.axis] = count.toInt()
                at + field.type.size
            }
            is RecordField.RescaleCount -> {
                val count = number(at, field.type, "the number of rescale pairs").toInt()
                if (count < 1 || count > MAX_VALUES) throw DecodeFailure("it says it has $count rescale pairs")
                rescaleCounts[field.axis] = count
                at + field.type.size
            }
            is RecordField.AxisOffset -> at.also { offsets[field.axis] = number(at, field.type, "axis ${axisName(field.axis)}'s offset") } + field.type.size
            is RecordField.AxisDistance -> at.also { distances[field.axis] = number(at, field.type, "axis ${axisName(field.axis)}'s step") } + field.type.size
            is RecordField.AxisShift -> {
                val shift = number(at, field.type, "axis ${axisName(field.axis)}'s shift").toInt()
                if (shift !in 0..62) throw DecodeFailure("axis ${axisName(field.axis)}'s shift is $shift")
                distances[field.axis] = (1L shl shift).toDouble()
                at + field.type.size
            }
            is RecordField.AxisPoints -> readPoints(field, at)
            is RecordField.Rescale -> readRescale(field, at)
            is RecordField.Values -> readValues(field, at)
            is RecordField.Skip -> at + field.type.size
        }
    }

    private fun readPoints(field: RecordField.AxisPoints, at: Long): Long {
        val axis = axis(field.axis)
        val count = countOf(field.axis)
        val stored = if (storage.static) maxOf(axis.maxPoints, count) else count
        if (stored > MAX_VALUES) throw DecodeFailure("axis ${axisName(field.axis)} keeps room for $stored points, more than $MAX_VALUES")
        val data = target(at, field.pointer)
        val addresses = LongArray(count) { i -> data + (if (field.decreasing) count - 1 - i else i).toLong() * field.type.size }
        var raw = DoubleArray(count) { i -> number(addresses[i], field.type, "axis ${axisName(field.axis)}'s point ${i + 1}") }
        if (axis.difference) raw = cumulative(raw)
        val pointsRange = AddressRange.ofSize(data, stored.toLong() * field.type.size)
        if (obj.kind == ObjectKind.AXIS) own(pointsRange, PartRole.VALUES) else own(pointsRange, PartRole.AXIS_POINTS, field.axis)
        pointRaw[field.axis] = raw
        pointSlots[field.axis] = Slots(addresses, field.type, order, difference = axis.difference)
        return if (field.pointer != null) at + field.pointer.size else at + stored.toLong() * field.type.size
    }

    private fun readRescale(field: RecordField.Rescale, at: Long): Long {
        val count = rescaleCounts[field.axis].takeIf { it >= 0 } ?: field.maxPairs
        if (count < 1) throw DecodeFailure("it has no rescale pairs")
        val stored = if (storage.static) maxOf(field.maxPairs, count) else count
        if (stored > MAX_VALUES) throw DecodeFailure("it keeps room for $stored rescale pairs, more than $MAX_VALUES")
        val data = target(at, field.pointer)
        val pairSize = 2L * field.type.size
        val pairAddresses = LongArray(count) { i -> data + (if (field.decreasing) count - 1 - i else i) * pairSize }
        rescalePairs = pairAddresses.mapIndexed { i, address ->
            number(address, field.type, "rescale pair ${i + 1}") to number(address + field.type.size, field.type, "rescale pair ${i + 1}")
        }
        rescaleSlots = Slots(pairAddresses, field.type, order)
        own(AddressRange.ofSize(data, stored * pairSize), PartRole.VALUES)
        return if (field.pointer != null) at + field.pointer.size else at + stored * pairSize
    }

    private fun readValues(field: RecordField.Values, at: Long): Long {
        val dimensions = valueDimensions()
        val memory = if (storage.static && obj.kind.axisCount > 0) {
            IntArray(dimensions.size) { maxOf(dimensions[it], obj.axes[it].maxPoints) }
        } else {
            dimensions
        }
        val total = cappedProduct(dimensions.asList()).toInt()
        val stored = cappedProduct(memory.asList())
        val data = target(at, field.pointer)
        // Memory strides in storage order: y first puts dimension 1 innermost
        val walk = if (field.order == ValueOrder.Y_FIRST && dimensions.size >= 2) listOf(1, 0) + (2 until dimensions.size) else dimensions.indices.toList()
        val strides = LongArray(dimensions.size)
        var stride = 1L
        for (d in walk) {
            strides[d] = stride
            stride *= memory[d]
        }
        val position = IntArray(dimensions.size)
        val addresses = LongArray(total)
        for (i in 0 until total) {
            var rest = i
            var offset = 0L
            for (d in dimensions.indices) {
                position[d] = rest % dimensions[d]
                rest /= dimensions[d]
                offset += position[d] * strides[d]
            }
            addresses[i] = data + offset * field.type.size
        }
        val mask = obj.bitMask?.takeIf { !field.type.float && it != 0L }
        valueRaw = DoubleArray(total) { i ->
            if (mask == null) {
                number(addresses[i], field.type, "value ${i + 1}")
            } else {
                read(addresses[i], field.type.size, "value ${i + 1}")
                val bits = image.readUnsigned(addresses[i], field.type.size, order).toLong()
                ((bits and mask) ushr mask.countTrailingZeroBits()).toDouble()
            }
        }
        valueSlots = Slots(addresses, field.type, order, mask)
        valueDimensions = dimensions
        own(AddressRange.ofSize(data, stored * field.type.size), PartRole.VALUES)
        return if (field.pointer != null) at + field.pointer.size else at + stored * field.type.size
    }

    private fun valueDimensions(): IntArray = when (obj.kind) {
        ObjectKind.VALUE -> IntArray(0)
        ObjectKind.VAL_BLK, ObjectKind.ASCII -> {
            val dimensions = obj.dimensions.ifEmpty { listOf(1) }
            if (dimensions.any { it < 1 }) throw DecodeFailure("its size ${dimensions.joinToString(" x ")} isn't valid")
            val total = cappedProduct(dimensions)
            if (obj.kind == ObjectKind.ASCII) intArrayOf(total.toInt()) else dimensions.toIntArray()
        }
        ObjectKind.AXIS -> throw DecodeFailure("an axis's record has values instead of points")
        else -> IntArray(obj.kind.axisCount) { countOf(it) }
    }

    /** An axis's number of points: read, fixed, or its maximum when neither is known yet */
    private fun countOf(axis: Int): Int {
        if (counts[axis] >= 0) return counts[axis]
        val max = axis(axis).maxPoints
        if (max <= 0) throw DecodeFailure("axis ${axisName(axis)}'s number of points isn't known")
        if (max > MAX_VALUES) throw DecodeFailure("axis ${axisName(axis)} can have $max points, more than $MAX_VALUES")
        if (axis in countedAxes) {
            warnings += Diagnostic(Severity.WARNING, "Axis ${axisName(axis)}'s count comes after what it sizes, so its maximum $max was used", obj.source, obj.name)
        }
        counts[axis] = max
        return max
    }

    // ------------------------------------------------ After the record

    private fun buildAxis(i: Int): AxisValues {
        external[i]?.let { return it }
        val axis = axis(i)
        val conversion = axis.conversion
        val raw = pointRaw[i] ?: offsets[i]?.let { offset ->
            val distance = distances[i] ?: throw DecodeFailure("axis ${axisName(i)} has an offset but no step")
            DoubleArray(countOf(i)) { offset + it * distance }
        }
        if (raw != null) {
            return AxisValues(axis, conversion, raw, DoubleArray(raw.size) { conversion.toPhysical(raw[it]) }, pointSlots[i])
        }
        if (obj.kind == ObjectKind.AXIS && rescalePairs != null) {
            val pairs = rescalePairs!!
            val values = DoubleArray(pairs.size) { pairs[it].first }
            val addresses = rescaleSlots!!
            return AxisValues(axis, conversion, values, DoubleArray(values.size) { conversion.toPhysical(values[it]) },
                Slots(addresses.addresses, addresses.type, order))
        }
        throw DecodeFailure("axis ${axisName(i)}'s points aren't in its record")
    }

    private fun axisObject(points: AxisValues): CalibrationValues = CalibrationValues(
        obj, start, intArrayOf(points.count), points.raw, points.physical, emptyList(), points.slots,
        rescaleIndexes = rescalePairs?.map { it.second }?.toDoubleArray(),
        ranges = ranges.merged(), parts = parts.toList(), warnings = warnings
    )

    private fun asciiText(): String {
        val slots = valueSlots?.takeIf { it.addresses.isNotEmpty() } ?: return ""
        val bytes = image.readOrNull(slots.addresses.first(), slots.addresses.size * slots.type.size) ?: return ""
        return TextCoding.decode(bytes, obj.encoding, order)
    }

    // ------------------------------------------------ Reading

    private fun align(address: Long, type: DataType): Long {
        val alignment = storage.alignment(type).toLong()
        return if (alignment <= 1) address else (address + alignment - 1) / alignment * alignment
    }

    /** Where a field's data is: right here, or where the pointer here points */
    private fun target(at: Long, pointer: DataType?): Long {
        if (pointer == null) return at
        val address = number(at, pointer, "a pointer").toLong()
        own(AddressRange.ofSize(at, pointer.size.toLong()), PartRole.RECORD)
        return address
    }

    private fun number(address: Long, type: DataType, what: String): Double {
        read(address, type.size, what)
        return image.readNumber(address, type, order)!!
    }

    private fun read(address: Long, size: Int, what: String) {
        if (!image.contains(address, size)) throw DecodeFailure("$what at ${address.toHexAddress()} isn't in the image")
    }

    /** The product of [sizes], stopping before it can overflow once it passes [MAX_VALUES] */
    private fun cappedProduct(sizes: List<Int>): Long {
        var product = 1L
        for (size in sizes) {
            if (size < 0) throw DecodeFailure("a size of $size isn't valid")
            if (size != 0 && product > MAX_VALUES / size) throw DecodeFailure("it would have more than $MAX_VALUES values")
            product *= size
        }
        return product
    }

    private fun axisName(i: Int) = AXIS_NAMES.getOrElse(i) { "${i + 1}" }

    companion object {
        /** More than any real map, and still small enough to hold */
        const val MAX_VALUES = 4_000_000
        val AXIS_NAMES = listOf("X", "Y", "Z", "4", "5")
    }
}

/** Points stored as differences: each is the one before plus its own */
private fun cumulative(differences: DoubleArray): DoubleArray {
    val points = DoubleArray(differences.size)
    var sum = 0.0
    for (i in differences.indices) {
        sum += differences[i]
        points[i] = sum
    }
    return points
}

/** y at [x], interpolated over points ([xs], [ys]) sorted by x and held at the ends */
private fun interpolate(xs: DoubleArray, ys: DoubleArray, x: Double): Double {
    if (xs.isEmpty()) return Double.NaN
    if (x <= xs.first()) return ys.first()
    if (x >= xs.last()) return ys.last()
    val upper = firstAtLeast(xs, x)
    val x0 = xs[upper - 1]
    val x1 = xs[upper]
    return if (x1 == x0) ys[upper - 1] else ys[upper - 1] + (ys[upper] - ys[upper - 1]) * (x - x0) / (x1 - x0)
}

/** For the codec's callers: all of an object's values as physical numbers, or why they couldn't be read */
fun CalibrationSet.decode(image: MemoryImage, obj: CalibrationObject, variant: Int? = null): TkResult<CalibrationValues, DecodeError> =
    CalibrationCodec.decode(image, obj, this, variant)

/** The first index of sorted [xs] whose value is at least [x], by binary search; [xs] size when there's none */
internal fun firstAtLeast(xs: DoubleArray, x: Double): Int {
    var low = 0
    var high = xs.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (xs[middle] < x) low = middle + 1 else high = middle
    }
    return low
}
