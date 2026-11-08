package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.math.abs

/**
 * A rectangle of cells in one 2D slice of an object's values: [x] and [y] are positions along the first two axes
 * (y is 0..0 for a curve, both for a single value), [outer] the fixed positions along the rest (z, 4th, 5th) of a cube
 */
data class CellSelection(val x: IntRange, val y: IntRange, val outer: List<Int> = emptyList()) {
    val width: Int get() = x.last - x.first + 1
    val height: Int get() = y.last - y.first + 1

    /** The value indices in it, row by row (y, then x) */
    fun indices(values: CalibrationValues): List<Int> = y.flatMap { yy -> x.map { xx -> values.indexAt(xx, yy, outer) } }

    companion object {
        /** From [from] to [to] (either way round) */
        fun between(from: Pair<Int, Int>, to: Pair<Int, Int>, outer: List<Int> = emptyList()) = CellSelection(
            minOf(from.first, to.first)..maxOf(from.first, to.first),
            minOf(from.second, to.second)..maxOf(from.second, to.second),
            outer
        )

        fun all(values: CalibrationValues, outer: List<Int> = emptyList()) =
            CellSelection(0 until values.columns, 0 until values.rows, outer)
    }
}

/** How many columns (x positions) a 2D slice has: 1 for a single value */
val CalibrationValues.columns: Int get() = dimensions.getOrElse(0) { 1 }

/** How many rows (y positions) a 2D slice has: 1 for a value or curve */
val CalibrationValues.rows: Int get() = dimensions.getOrElse(1) { 1 }

/** The index of the value at ([x], [y]) with the rest of the position [outer] */
fun CalibrationValues.indexAt(x: Int, y: Int, outer: List<Int> = emptyList()): Int {
    if (dimensions.isEmpty()) return 0
    val at = IntArray(dimensions.size)
    at[0] = x
    if (dimensions.size > 1) at[1] = y
    for (d in 2 until dimensions.size) at[d] = outer.getOrElse(d - 2) { 0 }
    return index(*at)
}

/** What the selection operations do */
enum class MapOperation(val label: String) {
    SET("Set to"),
    ADD("Add"),
    MULTIPLY("Multiply by"),
    ADD_PERCENT("Add %"),

    /** Straight lines (or a flat surface) between the selection's corners, by axis values when they rise */
    INTERPOLATE("Interpolate"),

    /** Each cell averaged with its neighbours (3 x 3) */
    SMOOTH("Smooth")
}

/**
 * The selection operations, as new physical values by index; nothing is written until [CalibrationCodec.writeAll].
 * Results aren't clamped here: writing does that, so it can say how many were
 */
object MapEdit {
    fun apply(values: CalibrationValues, selection: CellSelection, operation: MapOperation, amount: Double = 0.0): Map<Int, Double> {
        val cells = selection.indices(values)
        return when (operation) {
            MapOperation.SET -> cells.associateWith { amount }
            MapOperation.ADD -> cells.associateWith { values.physical[it] + amount }
            MapOperation.MULTIPLY -> cells.associateWith { values.physical[it] * amount }
            MapOperation.ADD_PERCENT -> cells.associateWith { values.physical[it] * (1 + amount / 100) }
            MapOperation.INTERPOLATE -> interpolate(values, selection)
            MapOperation.SMOOTH -> smooth(values, selection)
        }
    }

    /**
     * Bilinear between the four corners of the selection (linear along a single row or column). Positions are the
     * axis's physical points when they're strictly rising or falling, the cell positions otherwise
     */
    private fun interpolate(values: CalibrationValues, selection: CellSelection): Map<Int, Double> {
        val xs = positions(values, 0, selection.x)
        val ys = positions(values, 1, selection.y)
        fun corner(x: Int, y: Int) = values.physical[values.indexAt(x, y, selection.outer)]
        val q11 = corner(selection.x.first, selection.y.first)
        val q21 = corner(selection.x.last, selection.y.first)
        val q12 = corner(selection.x.first, selection.y.last)
        val q22 = corner(selection.x.last, selection.y.last)
        val result = mutableMapOf<Int, Double>()
        for ((j, y) in selection.y.withIndex()) {
            val ty = fraction(ys, j)
            for ((i, x) in selection.x.withIndex()) {
                val tx = fraction(xs, i)
                val top = q11 + (q21 - q11) * tx
                val bottom = q12 + (q22 - q12) * tx
                result[values.indexAt(x, y, selection.outer)] = top + (bottom - top) * ty
            }
        }
        return result
    }

    /** Where position [i] of [points] is between the first (0) and the last (1) */
    private fun fraction(points: DoubleArray, i: Int): Double {
        if (points.size < 2) return 0.0
        val span = points.last() - points.first()
        return if (span == 0.0) 0.0 else (points[i] - points.first()) / span
    }

    /** The axis points over [range], or the positions themselves when they don't rise or fall steadily */
    private fun positions(values: CalibrationValues, axis: Int, range: IntRange): DoubleArray {
        val fallback = DoubleArray(range.count()) { (range.first + it).toDouble() }
        val points = values.axes.getOrNull(axis)?.physical ?: return fallback
        if (range.last >= points.size) return fallback
        val slice = DoubleArray(range.count()) { points[range.first + it] }
        val rising = (1 until slice.size).all { slice[it] > slice[it - 1] }
        val falling = (1 until slice.size).all { slice[it] < slice[it - 1] }
        return if (rising || falling) slice else fallback
    }

    /** Each selected cell becomes the mean of itself and its neighbours in the slice, from the values before */
    private fun smooth(values: CalibrationValues, selection: CellSelection): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        for (y in selection.y) {
            for (x in selection.x) {
                var sum = 0.0
                var count = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until values.columns && ny in 0 until values.rows) {
                        sum += values.physical[values.indexAt(nx, ny, selection.outer)]
                        count++
                    }
                }
                result[values.indexAt(x, y, selection.outer)] = sum / count
            }
        }
        return result
    }

    /**
     * Where pasted [grid] (rows of y, columns of x, as copied from a spreadsheet) goes: from the selection's top left,
     * cut off at the edges. A single pasted value fills the whole selection. Blank cells are left alone
     */
    fun paste(values: CalibrationValues, selection: CellSelection, grid: List<List<Double?>>): Map<Int, Double> {
        val single = grid.singleOrNull()?.singleOrNull()
        if (single != null) return selection.indices(values).associateWith { single }
        val result = mutableMapOf<Int, Double>()
        grid.forEachIndexed { row, cells ->
            val y = selection.y.first + row
            if (y >= values.rows) return@forEachIndexed
            cells.forEachIndexed { column, value ->
                val x = selection.x.first + column
                if (value != null && x < values.columns) result[values.indexAt(x, y, selection.outer)] = value
            }
        }
        return result
    }

    /** The selection's values as rows for copying, formatted plainly */
    fun copy(values: CalibrationValues, selection: CellSelection): List<List<String>> =
        selection.y.map { y -> selection.x.map { x -> plainNumber(values.physical[values.indexAt(x, y, selection.outer)]) } }
}

/**
 * What writing several values did
 * @property written How many were written
 * @property clamped How many were outside the limits (or the type's range) and were written at the nearest end
 * @property failed The ones that couldn't be written at all, and why
 */
class WriteSummary(val written: Int, val clamped: Int, val failed: Map<Int, EncodeError>) {
    val ok: Boolean get() = failed.isEmpty()
}

/**
 * Writes many values (index to physical) as one undo step. Values past the limits are clamped to them (to the
 * extended limits when [extended]), raw values to the type's range, and each is rounded to the type
 */
fun CalibrationCodec.writeAll(
    image: MemoryImage,
    values: CalibrationValues,
    changes: Map<Int, Double>,
    extended: Boolean = false
): WriteSummary {
    val obj = values.obj
    val slots = values.slots
    if (slots == null || obj.readOnly) return WriteSummary(0, 0, changes.mapValues { EncodeError.READ_ONLY })
    val limits = if (extended) obj.extendedLimits ?: (obj.lowerLimit..obj.upperLimit) else obj.lowerLimit..obj.upperLimit
    var written = 0
    var clamped = 0
    val failed = mutableMapOf<Int, EncodeError>()
    image.edit {
        for ((index, wanted) in changes) {
            if (index !in 0 until values.size) {
                failed[index] = EncodeError.NO_SUCH_VALUE
                continue
            }
            if (wanted.isNaN()) {
                failed[index] = EncodeError.NOT_A_NUMBER
                continue
            }
            val physical = wanted.coerceIn(limits.start, limits.endInclusive)
            var outside = physical != wanted
            val range = slots.bitMask?.let { 0.0..(it ushr it.countTrailingZeroBits()).toDouble() } ?: slots.type.range
            var raw = obj.conversion.toRaw(physical, range)
            if (raw.isNaN() || raw.isInfinite()) {
                failed[index] = EncodeError.NO_RAW_VALUE
                continue
            }
            if (raw !in range) {
                raw = raw.coerceIn(range.start, range.endInclusive)
                outside = true
            }
            when (val result = writeRaw(this, values, index, raw)) {
                is org.mr0btain.tunerstoolkit.core.error.TkResult.Success -> {
                    written++
                    if (outside) clamped++
                }
                is org.mr0btain.tunerstoolkit.core.error.TkResult.Failure -> failed[index] = result.error
            }
        }
    }
    return WriteSummary(written, clamped, failed)
}

/** A number as plain text for copying: up to six decimals, no trailing zeros */
fun plainNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()
    val text = formatValue(value, "%.6")
    return if ('.' in text) text.trimEnd('0').trimEnd('.') else text
}

/**
 * A number as text that reads back as exactly the same double: plain decimals when they do, the shortest exact form
 * (which may have an exponent) otherwise. For files that are read back, not for people
 */
fun exactNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()
    val plain = plainNumber(value)
    if (plain.toDoubleOrNull() == value) return plain
    return value.toString()
}

/** Tab separated text, as spreadsheets copy and paste it */
object Tsv {
    fun format(rows: List<List<String>>): String = rows.joinToString("\n") { it.joinToString("\t") }

    /**
     * Rows of numbers from spreadsheet text. A comma is taken as the decimal point (cells are split on tabs, so it
     * can't be a separator); a cell that isn't a number is null
     */
    fun parse(text: String): List<List<Double?>> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n').split('\n')
        if (lines.size == 1 && lines[0].isBlank()) return emptyList()
        return lines.map { line -> line.split('\t').map(::number) }
    }

    private fun number(cell: String): Double? {
        val text = cell.trim().replace(" ", "").replace(" ", "")
        if (text.isEmpty()) return null
        text.toDoubleOrNull()?.let { return it }
        // 1,5 or 1.234,5
        val european = if (text.count { it == ',' } == 1) text.replace(".", "").replace(',', '.') else null
        return european?.toDoubleOrNull()
    }
}

/** The smallest physical change one raw step makes at [index], for nudging a value up or down */
fun CalibrationValues.step(index: Int): Double {
    val here = physical[index]
    val up = obj.conversion.toPhysical(raw[index] + 1)
    val down = obj.conversion.toPhysical(raw[index] - 1)
    val steps = listOf(up - here, here - down).map(::abs).filter { it > 0 && !it.isNaN() }
    return steps.minOrNull() ?: 1.0
}
