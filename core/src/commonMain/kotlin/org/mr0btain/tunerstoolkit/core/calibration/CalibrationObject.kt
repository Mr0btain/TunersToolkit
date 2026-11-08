package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/*
 * The one definition model every format maps into (A2L now; XDF, our own definitions and the map finder later): what
 * an object is, where it is, how its record is laid out, its axes and its conversion, with every name resolved.
 * CalibrationCodec reads one from a MemoryImage into CalibrationValues and writes edits back.
 */

/** What kind of object it is: how many axes it has, or whether it's a block, text or an axis of its own */
enum class ObjectKind(val axisCount: Int) {
    VALUE(0), CURVE(1), MAP(2), CUBOID(3), CUBE_4(4), CUBE_5(5),

    /** An array of values without axes, one to three dimensions */
    VAL_BLK(0),

    /** Text */
    ASCII(0),

    /** An axis shared by several curves and maps; its points are its values */
    AXIS(1)
}

/**
 * The order values are stored in. [X_FIRST] walks along x first, so value (x, y) is at y * xCount + x (A2L's ROW_DIR);
 * [Y_FIRST] walks along y first, x * yCount + y (COLUMN_DIR). Axes past y are always outermost
 */
enum class ValueOrder { X_FIRST, Y_FIRST }

/**
 * One part of an object's record, in the order it's stored. [axis] counts from 0 for x; the space each takes depends on
 * the counts read before it (or the maximum, for a [Storage.static] record)
 */
sealed interface RecordField {
    /** The type the field is stored as, which is also what it's aligned to */
    val type: DataType

    /**
     * The values. With a [pointer] the record holds a pointer of that type to them instead
     */
    class Values(override val type: DataType, val order: ValueOrder, val pointer: DataType? = null) : RecordField

    /** An axis's points. [decreasing] when they're stored last point first */
    class AxisPoints(val axis: Int, override val type: DataType, val decreasing: Boolean = false, val pointer: DataType? = null) : RecordField

    /** How many points an axis has */
    class AxisCount(val axis: Int, override val type: DataType) : RecordField

    /**
     * A rescale axis's pairs of (axis value, virtual index), up to [maxPairs]. The axis it describes has a point at
     * each virtual index, interpolated between the pairs
     */
    class Rescale(val axis: Int, override val type: DataType, val maxPairs: Int, val decreasing: Boolean = false, val pointer: DataType? = null) : RecordField

    /** How many pairs a [Rescale] has */
    class RescaleCount(val axis: Int, override val type: DataType) : RecordField

    /** An axis given as its first point and the step between points: point i is offset + i * distance */
    class AxisOffset(val axis: Int, override val type: DataType) : RecordField
    class AxisDistance(val axis: Int, override val type: DataType) : RecordField

    /** The step as a power of two: distance = 2^shift */
    class AxisShift(val axis: Int, override val type: DataType) : RecordField

    /** Space the record keeps for something that isn't shown, like A2L's SRC_ADDR, RIP_ADDR and RESERVED */
    class Skip(override val type: DataType, val what: String) : RecordField
}

/**
 * How an object's record is stored
 * @property alignments Each type's alignment in bytes; types not in it are aligned to their size
 * @property static The record keeps room for every axis's maximum number of points, so nothing moves when a count changes
 */
class Storage(
    val fields: List<RecordField>,
    val byteOrder: ByteOrder,
    val alignments: Map<DataType, Int> = emptyMap(),
    val static: Boolean = false
) {
    fun alignment(type: DataType): Int = alignments[type] ?: type.size
    val values: RecordField.Values? get() = fields.firstNotNullOfOrNull { it as? RecordField.Values }
}

/** Where an axis's points come from */
sealed interface AxisSource {
    /** Stored in the object's own record: its points, or an offset and step */
    data object Record : AxisSource

    /** An [ObjectKind.AXIS] object of its own, shared with other objects */
    class Shared(val name: String) : AxisSource

    /** An [ObjectKind.AXIS] object holding rescale pairs */
    class Rescaled(val name: String) : AxisSource

    /** Points that aren't in memory at all: raw values, converted with the axis's conversion like stored ones */
    class Fixed(val points: List<Double>) : AxisSource

    /** A curve elsewhere maps the input to this axis's index, so the points are 0, 1, 2, … */
    class Curve(val name: String) : AxisSource
}

/**
 * One axis of a curve, map or cube (or the points of an AXIS object)
 * @property input The measurement the ECU looks it up with
 * @property maxPoints The most points it can have; 0 when unknown
 * @property fixedCount A count fixed by the definition instead of stored in the record
 * @property difference Points are stored as the difference from the one before (the first as itself)
 */
class CalibrationAxis(
    val source: AxisSource,
    val conversion: Conversion,
    val maxPoints: Int,
    val input: String? = null,
    val unit: String = "",
    val format: String? = null,
    val lowerLimit: Double = Double.NEGATIVE_INFINITY,
    val upperLimit: Double = Double.POSITIVE_INFINITY,
    val fixedCount: Int? = null,
    val difference: Boolean = false
)

/**
 * A calibration object with everything resolved
 * @property address Where its record starts, with any calibration offset already added
 * @property dimensions The size of a [ObjectKind.VAL_BLK] (x, then y and z) or the length of [ObjectKind.ASCII] text;
 * other kinds take theirs from their axes
 * @property bitMask Only these bits of each stored value belong to it
 * @property variantAddresses Where the record is for each variant, when it depends on variant coding
 * @property encoding For text: UTF8 (also plain ASCII), UTF16 or UTF32
 * @property source Where it's defined, for diagnostics
 */
class CalibrationObject(
    val name: String,
    val description: String,
    val kind: ObjectKind,
    val address: Long,
    val storage: Storage,
    val conversion: Conversion,
    val axes: List<CalibrationAxis> = emptyList(),
    val dimensions: List<Int> = emptyList(),
    val unit: String = "",
    val format: String? = null,
    val displayName: String? = null,
    val lowerLimit: Double = Double.NEGATIVE_INFINITY,
    val upperLimit: Double = Double.POSITIVE_INFINITY,
    val extendedLimits: ClosedFloatingPointRange<Double>? = null,
    val readOnly: Boolean = false,
    val bitMask: Long? = null,
    val encoding: String? = null,
    val variantAddresses: List<Long> = emptyList(),
    val source: SourceLocation? = null
) {
    override fun toString(): String = "$kind $name"
}

/**
 * Every object a definition describes, by name, and what was wrong when making them
 */
class CalibrationSet(val objects: List<CalibrationObject>, val diagnostics: List<Diagnostic> = emptyList()) {
    private val byName = objects.asReversed().associateBy { it.name }

    operator fun get(name: String): CalibrationObject? = byName[name]
}
