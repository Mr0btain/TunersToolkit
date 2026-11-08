package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/**
 * A map defined by hand, the way a tuner describes one found in a binary without a definition: where it is, how
 * it's stored, how to scale it, and where its axes come from. [objects] makes the calibration objects
 */
data class ManualMap(
    val name: String,
    val description: String = "",
    val kind: ObjectKind = ObjectKind.MAP,
    val address: Long,
    val type: DataType = DataType.UBYTE,
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    /** x points (columns); 1 for a value */
    val columns: Int = 1,
    /** y points (rows); 1 for a value or curve */
    val rows: Int = 1,
    /** How the values are stored: along x first (row by row) or y first */
    val order: ValueOrder = ValueOrder.X_FIRST,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val decimals: Int = 2,
    val lower: Double? = null,
    val upper: Double? = null,
    val x: ManualAxis = ManualAxis(),
    val y: ManualAxis = ManualAxis()
) {
    /** Why it can't be made, or null */
    fun problem(): String? = when {
        name.isBlank() -> "It needs a name"
        kind !in KINDS -> "Only values, curves, maps and blocks can be made by hand"
        columns !in 1..MAX_POINTS || rows !in 1..MAX_POINTS -> "Up to $MAX_POINTS points along each axis"
        factor == 0.0 || factor.isNaN() -> "The factor can't be 0"
        address < 0 -> "The address can't be negative"
        else -> listOf(x, y).firstNotNullOfOrNull { it.problem() }
    }

    /** The map, and an AXIS object for each axis stored in memory (named "<name> X axis" and so on) */
    fun objects(): List<CalibrationObject> {
        val axes = when (kind) {
            ObjectKind.CURVE -> listOf("X" to (x to columns))
            ObjectKind.MAP -> listOf("X" to (x to columns), "Y" to (y to rows))
            else -> emptyList()
        }
        val shared = mutableListOf<CalibrationObject>()
        val calibrationAxes = axes.map { (letter, pair) ->
            val (axis, count) = pair
            val conversion = linear(axis.factor, axis.offset)
            when (axis.source) {
                ManualAxis.Source.POSITIONS -> CalibrationAxis(AxisSource.Fixed(List(count) { it.toDouble() }), Conversion.Identical, count, unit = axis.unit)
                ManualAxis.Source.EVEN -> CalibrationAxis(
                    AxisSource.Fixed(List(count) { i -> if (count == 1) axis.from else axis.from + (axis.to - axis.from) * i / (count - 1) }),
                    Conversion.Identical, count, unit = axis.unit, format = "%.${axis.decimals}"
                )
                ManualAxis.Source.MEMORY -> {
                    val axisName = "$name $letter axis"
                    shared += CalibrationObject(
                        name = axisName,
                        description = "",
                        kind = ObjectKind.AXIS,
                        address = axis.address,
                        storage = Storage(listOf(RecordField.AxisPoints(0, axis.type)), axis.byteOrder),
                        conversion = conversion,
                        axes = listOf(CalibrationAxis(AxisSource.Record, conversion, count, unit = axis.unit, format = "%.${axis.decimals}", fixedCount = count)),
                        unit = axis.unit,
                        format = "%.${axis.decimals}"
                    )
                    CalibrationAxis(AxisSource.Shared(axisName), conversion, count, unit = axis.unit, format = "%.${axis.decimals}")
                }
            }
        }
        val map = CalibrationObject(
            name = name.trim(),
            description = description,
            kind = kind,
            address = address,
            storage = Storage(listOf(RecordField.Values(type, order)), byteOrder),
            conversion = linear(factor, offset),
            axes = calibrationAxes,
            dimensions = if (kind == ObjectKind.VAL_BLK) (if (rows == 1) listOf(columns) else listOf(columns, rows)) else emptyList(),
            unit = unit,
            format = "%.$decimals",
            lowerLimit = lower ?: Double.NEGATIVE_INFINITY,
            upperLimit = upper ?: Double.POSITIVE_INFINITY
        )
        return listOf(map) + shared
    }

    companion object {
        const val MAX_POINTS = 1024
        val KINDS = listOf(ObjectKind.VALUE, ObjectKind.CURVE, ObjectKind.MAP, ObjectKind.VAL_BLK)

        private fun linear(factor: Double, offset: Double): Conversion =
            if (factor == 1.0 && offset == 0.0) Conversion.Identical else Conversion.Linear(factor, offset)

        /**
         * The manual map [obj] was made from, so it can be edited; null when it wasn't made this way (or has been
         * changed past what the form can show)
         */
        fun of(obj: CalibrationObject, set: CalibrationSet): ManualMap? {
            if (obj.kind !in KINDS) return null
            val values = obj.storage.fields.singleOrNull() as? RecordField.Values ?: return null
            if (values.pointer != null || obj.bitMask != null) return null
            val (factor, offset) = factorOffset(obj.conversion) ?: return null
            val axes = obj.axes.map { axis -> ManualAxis.of(axis, set) ?: return null }
            return ManualMap(
                name = obj.name,
                description = obj.description,
                kind = obj.kind,
                address = obj.address,
                type = values.type,
                byteOrder = obj.storage.byteOrder,
                columns = obj.axes.getOrNull(0)?.maxPoints ?: obj.dimensions.getOrElse(0) { 1 },
                rows = obj.axes.getOrNull(1)?.maxPoints ?: obj.dimensions.getOrElse(1) { 1 },
                order = values.order,
                factor = factor,
                offset = offset,
                unit = obj.unit,
                decimals = decimalsOf(obj.format) ?: 2,
                lower = obj.lowerLimit.takeIf { it.isFinite() },
                upper = obj.upperLimit.takeIf { it.isFinite() },
                x = axes.getOrElse(0) { ManualAxis() },
                y = axes.getOrElse(1) { ManualAxis() }
            )
        }

        internal fun factorOffset(conversion: Conversion): Pair<Double, Double>? = when (conversion) {
            Conversion.Identical -> 1.0 to 0.0
            is Conversion.Linear -> conversion.factor to conversion.offset
            else -> null
        }
    }
}

/** Where a hand made map's axis comes from */
data class ManualAxis(
    val source: Source = Source.POSITIONS,
    /** For [Source.EVEN]: the first and last points, the rest spread evenly between */
    val from: Double = 0.0,
    val to: Double = 0.0,
    /** For [Source.MEMORY] */
    val address: Long = 0,
    val type: DataType = DataType.UBYTE,
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    val factor: Double = 1.0,
    val offset: Double = 0.0,
    val unit: String = "",
    val decimals: Int = 0
) {
    enum class Source(val label: String) {
        POSITIONS("0, 1, 2…"),
        EVEN("Evenly spaced"),
        MEMORY("In memory")
    }

    fun problem(): String? = when {
        source == Source.MEMORY && (factor == 0.0 || factor.isNaN()) -> "An axis factor can't be 0"
        source == Source.MEMORY && address < 0 -> "An axis address can't be negative"
        else -> null
    }

    companion object {
        fun of(axis: CalibrationAxis, set: CalibrationSet): ManualAxis? = when (val source = axis.source) {
            is AxisSource.Fixed -> {
                val points = source.points
                val even = points.size > 1 && points.indices.drop(1).all { kotlin.math.abs((points[it] - points[it - 1]) - (points[1] - points[0])) < 1e-9 }
                when {
                    points.withIndex().all { it.value == it.index.toDouble() } -> ManualAxis(unit = axis.unit)
                    even || points.size == 1 -> ManualAxis(Source.EVEN, points.first(), points.last(), unit = axis.unit, decimals = decimalsOf(axis.format) ?: 0)
                    else -> null
                }
            }
            is AxisSource.Shared -> {
                val shared = set[source.name] ?: return null
                val field = shared.storage.fields.singleOrNull() as? RecordField.AxisPoints ?: return null
                val (factor, offset) = ManualMap.factorOffset(shared.conversion) ?: return null
                ManualAxis(Source.MEMORY, address = shared.address, type = field.type, byteOrder = shared.storage.byteOrder, factor = factor, offset = offset,
                    unit = shared.unit, decimals = decimalsOf(shared.format) ?: 0)
            }
            else -> null
        }
    }
}
