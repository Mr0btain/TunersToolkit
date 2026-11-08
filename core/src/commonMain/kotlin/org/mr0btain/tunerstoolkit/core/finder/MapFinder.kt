package org.mr0btain.tunerstoolkit.core.finder

import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ManualAxis
import org.mr0btain.tunerstoolkit.core.calibration.ManualMap
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import kotlin.math.abs

/**
 * What to look for
 * @property axisTypes How axis points (and counts) may be stored; whole numbers only, since axes have to rise
 * @property valueTypes How values may be stored
 * @property skip Memory not to suggest maps in, usually what a definition already covers
 * @property bare Also look for axes without counts in front of them (weaker evidence, more suggestions)
 */
data class FinderOptions(
    val byteOrders: List<ByteOrder> = ByteOrder.entries,
    val axisTypes: List<DataType> = listOf(DataType.UBYTE, DataType.UWORD),
    val valueTypes: List<DataType> = listOf(DataType.UBYTE, DataType.UWORD, DataType.SWORD),
    val minPoints: Int = 4,
    val maxPoints: Int = 32,
    val minConfidence: Int = 55,
    val ranges: List<AddressRange>? = null,
    val skip: List<AddressRange> = emptyList(),
    val bare: Boolean = true
)

/** An axis found in memory: where its points are, how they're stored, and their raw values */
class FoundAxis(val address: Long, val type: DataType, val byteOrder: ByteOrder, val points: DoubleArray) {
    val range: AddressRange get() = AddressRange.ofSize(address, points.size.toLong() * type.size)
}

/**
 * A map (or curve) the finder thinks is there
 * @property address Where its values start
 * @property counted Its axes had counts in front of them, which agreed with them: the strongest evidence
 * @property values The raw values, x first
 * @property record All the memory it takes: counts, axes and values
 * @property confidence 0 to 100
 * @property reasons Why it scored what it did
 */
class FoundMap(
    val kind: ObjectKind,
    val address: Long,
    val valueType: DataType,
    val byteOrder: ByteOrder,
    val columns: Int,
    val rows: Int,
    val order: ValueOrder,
    val x: FoundAxis,
    val y: FoundAxis?,
    val counted: Boolean,
    val values: DoubleArray,
    val record: AddressRange,
    val confidence: Int,
    val reasons: List<String>
) {
    val label: String
        get() = "${if (kind == ObjectKind.MAP) "$columns × $rows map" else "$columns point curve"} of ${valueType.name} at ${address.toHexAddress()}"

    /** As a map made by hand, raw values with factor 1, for confirming; scaling it is up to the person */
    fun toManualMap(name: String = "Found ${address.toHexAddress()}"): ManualMap = ManualMap(
        name = name,
        description = "Found by the map finder ($confidence%)",
        kind = kind,
        address = address,
        type = valueType,
        byteOrder = byteOrder,
        columns = columns,
        rows = rows,
        order = order,
        decimals = 0,
        x = axisOf(x),
        y = y?.let(::axisOf) ?: ManualAxis()
    )

    private fun axisOf(axis: FoundAxis) = ManualAxis(ManualAxis.Source.MEMORY, address = axis.address, type = axis.type, byteOrder = axis.byteOrder)
}

/**
 * Looks through an image for calibration maps without a definition. It knows two shapes:
 *
 * - **Counted records**, as Bosch and others store maps with their own axes: the axes' point counts, then their points
 *   (rising), then the values; counts before both axes (nx ny x… y… values) or each before its own (nx x… ny y…). A
 *   curve is n x… values. The counts having to match rising axes makes these strong evidence.
 * - **Bare axes**: a rising run of numbers (or two, back to back) followed by a block of values of exactly the right
 *   size. Much weaker; the values have to look like a map.
 *
 * Values are scored by how smooth they are along both directions (which also picks row or column order), and blocks of
 * erased flash (all 0xFF) or of one value score down. Overlapping suggestions keep the best one. It's heuristics:
 * the suggestions need a person's eye before they're used
 */
object MapFinder {
    /**
     * @param progress Called now and then with how far it's got, 0 to 1
     * @param cancelled Checked now and then; true stops it with what it has found so far
     */
    fun find(image: MemoryImage, options: FinderOptions = FinderOptions(), progress: (Double) -> Unit = {}, cancelled: () -> Boolean = { false }): List<FoundMap> {
        val found = ArrayList<FoundMap>()
        val segments = image.ranges.withIndex().filter { (_, range) -> options.ranges?.any { it.overlaps(range) } ?: true }
        val total = segments.sumOf { it.value.size }.coerceAtLeast(1).toDouble()
        var done = 0L
        for ((index, range) in segments) {
            val bytes = image.segmentBytes(index)
            val segment = Segment(range.start, bytes)
            for (order in options.byteOrders) {
                for (axisType in options.axisTypes) {
                    if (cancelled()) return choose(found, options)
                    scanCounted(segment, axisType, order, options, found, cancelled)
                    if (options.bare) scanBare(segment, axisType, order, options, found)
                }
            }
            done += range.size
            progress(done / total)
        }
        return choose(found, options)
    }

    /** The best suggestions that don't overlap each other, what's skipped or the ranges asked for */
    private fun choose(found: List<FoundMap>, options: FinderOptions): List<FoundMap> {
        val chosen = ArrayList<FoundMap>()
        for (map in found.sortedWith(compareByDescending<FoundMap> { it.confidence }.thenBy { it.address })) {
            if (map.confidence < options.minConfidence) continue
            if (options.skip.any { it.overlaps(map.record) }) continue
            if (options.ranges != null && options.ranges.none { it.contains(map.record) }) continue
            if (chosen.any { it.record.overlaps(map.record) }) continue
            chosen += map
        }
        return chosen.sortedBy { it.address }
    }

    /** A segment's bytes, read as numbers without going back to the image */
    private class Segment(val start: Long, val bytes: ByteArray) {
        val end: Long get() = start + bytes.size

        fun has(address: Long, size: Long): Boolean = address >= start && address + size <= end

        fun read(address: Long, type: DataType, order: ByteOrder): Double {
            val offset = (address - start).toInt()
            val raw = order.decode(bytes, offset, type.size)
            return if (type.signed && type.size < 8) {
                val shift = 64 - type.size * 8
                ((raw.toLong() shl shift) shr shift).toDouble()
            } else {
                raw.toLong().toDouble()
            }
        }

        fun readMany(address: Long, count: Int, type: DataType, order: ByteOrder): DoubleArray? {
            if (count <= 0 || !has(address, count.toLong() * type.size)) return null
            return DoubleArray(count) { read(address + it.toLong() * type.size, type, order) }
        }
    }

    // ------------------------------------------------ Counted records

    private fun scanCounted(segment: Segment, type: DataType, order: ByteOrder, options: FinderOptions, out: MutableList<FoundMap>, cancelled: () -> Boolean) {
        val size = type.size.toLong()
        var at = align(segment.start, type.size)
        var steps = 0
        while (segment.has(at, size * 2)) {
            if (++steps % 65536 == 0 && cancelled()) return
            val first = segment.read(at, type, order).toInt()
            if (first in options.minPoints..options.maxPoints) {
                // nx ny x… y…
                val second = segment.read(at + size, type, order).toInt()
                if (second in options.minPoints..options.maxPoints) {
                    val x = axis(segment, at + 2 * size, first, type, order)
                    val y = x?.let { axis(segment, it.range.endExclusive, second, type, order) }
                    if (x != null && y != null) mapAfter(segment, x, y, y.range.endExclusive, true, AddressRange(at, at), options, out)
                }
                // nx x… ny y…, or a curve n x… values
                val x = axis(segment, at + size, first, type, order)
                if (x != null) {
                    val ny = segment.takeIf { it.has(x.range.endExclusive, size) }?.read(x.range.endExclusive, type, order)?.toInt()
                    if (ny != null && ny in options.minPoints..options.maxPoints) {
                        val y = axis(segment, x.range.endExclusive + size, ny, type, order)
                        if (y != null) mapAfter(segment, x, y, y.range.endExclusive, true, AddressRange(at, at), options, out)
                    }
                    curveAfter(segment, x, x.range.endExclusive, true, AddressRange(at, at), options, out)
                }
            }
            at += size
        }
    }

    /** [count] strictly rising points of [type] at [address], or null */
    private fun axis(segment: Segment, address: Long, count: Int, type: DataType, order: ByteOrder): FoundAxis? {
        val points = segment.readMany(address, count, type, order) ?: return null
        for (i in 1 until points.size) if (points[i] <= points[i - 1]) return null
        return FoundAxis(address, type, order, points)
    }

    // ------------------------------------------------ Bare axes

    private fun scanBare(segment: Segment, type: DataType, order: ByteOrder, options: FinderOptions, out: MutableList<FoundMap>) {
        val size = type.size.toLong()
        val start = align(segment.start, type.size)
        val count = ((segment.end - start) / size).toInt()
        if (count < options.minPoints) return
        val numbers = DoubleArray(count) { segment.read(start + it * size, type, order) }
        // Maximal strictly rising runs (long ones too: an axis's run can carry on into whatever follows it)
        val runs = ArrayList<IntRange>()
        var runStart = 0
        for (i in 1..count) {
            if (i == count || numbers[i] <= numbers[i - 1]) {
                if (i - runStart >= options.minPoints) runs += runStart until i
                runStart = i
            }
        }
        // A run's points can be the axis, or its first few can be and the rest belong to what follows; every length
        // is tried and the best reading wins when suggestions are chosen
        fun lengths(run: IntRange) = options.minPoints..minOf(options.maxPoints, run.last - run.first + 1)
        for ((n, run) in runs.withIndex()) {
            val next = runs.getOrNull(n + 1)?.takeIf { it.first == run.last + 1 }
            for (xLength in lengths(run)) {
                // Only the whole run can be followed by the next one; shorter, the rest is the curve's values
                val x = FoundAxis(start + run.first * size, type, order, numbers.copyOfRange(run.first, run.first + xLength))
                if (!plausibleBareAxis(x.points)) continue
                curveAfter(segment, x, x.range.endExclusive, false, x.range, options, out)
                if (xLength != run.last - run.first + 1 || next == null) continue
                for (yLength in lengths(next)) {
                    val y = FoundAxis(start + next.first * size, type, order, numbers.copyOfRange(next.first, next.first + yLength))
                    if (plausibleBareAxis(y.points)) mapAfter(segment, x, y, y.range.endExclusive, false, x.range, options, out)
                }
            }
        }
    }

    /** Positions 0, 1, 2… and runs from tables of addresses look like axes but rarely are */
    private fun plausibleBareAxis(points: DoubleArray): Boolean {
        val steps = (1 until points.size).map { points[it] - points[it - 1] }
        if (steps.all { it == 1.0 }) return false
        return points.last() - points.first() >= points.size
    }

    // ------------------------------------------------ Values after axes

    private fun mapAfter(segment: Segment, x: FoundAxis, y: FoundAxis, after: Long, counted: Boolean, head: AddressRange, options: FinderOptions, out: MutableList<FoundMap>) {
        val n = x.points.size * y.points.size
        for (valueType in options.valueTypes) {
            val address = align(after, valueType.size)
            val raw = segment.readMany(address, n, valueType, x.byteOrder) ?: continue
            val rows = y.points.size
            val columns = x.points.size
            // Which way round they're stored: the smoother reading wins
            val byRows = DoubleArray(n) { raw[it] }
            val byColumns = DoubleArray(n) { i -> raw[(i % columns) * rows + i / columns] }
            val rowScore = smoothness(byRows, columns, rows)
            val columnScore = smoothness(byColumns, columns, rows)
            val (order, values, smooth) = if (columnScore < rowScore) Triple(ValueOrder.Y_FIRST, byColumns, columnScore) else Triple(ValueOrder.X_FIRST, byRows, rowScore)
            val start = if (counted) head.start else x.address
            val record = AddressRange(start, address + n.toLong() * valueType.size)
            val (confidence, reasons) = score(values, valueType, smooth, counted, listOf(x, y))
            out += FoundMap(ObjectKind.MAP, address, valueType, x.byteOrder, columns, rows, order, x, y, counted, values, record, confidence, reasons)
        }
    }

    private fun curveAfter(segment: Segment, x: FoundAxis, after: Long, counted: Boolean, head: AddressRange, options: FinderOptions, out: MutableList<FoundMap>) {
        val n = x.points.size
        // Another counted axis straight after is two axes in a row (shared axes), not a curve's values
        if (counted && startsCountedAxis(segment, after, x.type, x.byteOrder, options)) return
        for (valueType in options.valueTypes) {
            val address = align(after, valueType.size)
            val values = segment.readMany(address, n, valueType, x.byteOrder) ?: continue
            val smooth = smoothness(values, n, 1)
            val start = if (counted) head.start else x.address
            val record = AddressRange(start, address + n.toLong() * valueType.size)
            val (score, reasons) = score(values, valueType, smooth, counted, listOf(x))
            // A curve is a weaker claim than a map: fewer numbers have to agree
            out += FoundMap(ObjectKind.CURVE, address, valueType, x.byteOrder, n, 1, ValueOrder.X_FIRST, x, null, counted, values, record, (score - 10).coerceAtLeast(0), reasons)
        }
    }

    private fun startsCountedAxis(segment: Segment, at: Long, type: DataType, order: ByteOrder, options: FinderOptions): Boolean {
        val address = align(at, type.size)
        if (!segment.has(address, type.size.toLong())) return false
        val count = segment.read(address, type, order).toInt()
        return count in options.minPoints..options.maxPoints && axis(segment, address + type.size, count, type, order) != null
    }

    /**
     * How rough the values are: the mean step between neighbours along x (and y) over their span. Smooth maps are
     * well under 0.2; random bytes are about 0.33 along each direction
     */
    internal fun smoothness(values: DoubleArray, columns: Int, rows: Int): Double {
        // The span between the 10th and 90th percentiles, so a few wild values (noise taken in at an edge) can't
        // stretch it and make every step look small
        val sorted = values.sortedArray()
        val robust = sorted[(sorted.size * 9 / 10).coerceAtMost(sorted.size - 1)] - sorted[sorted.size / 10]
        val span = if (robust > 0) robust else sorted.last() - sorted.first()
        if (span == 0.0) return 0.0
        var sum = 0.0
        var count = 0
        for (y in 0 until rows) for (x in 0 until columns) {
            val here = values[y * columns + x]
            if (x + 1 < columns) { sum += abs(values[y * columns + x + 1] - here); count++ }
            if (y + 1 < rows) { sum += abs(values[(y + 1) * columns + x] - here); count++ }
        }
        return if (count == 0) 1.0 else sum / count / span
    }

    private fun score(values: DoubleArray, type: DataType, smooth: Double, counted: Boolean, axes: List<FoundAxis>): Pair<Int, List<String>> {
        val reasons = mutableListOf<String>()
        var score = if (counted) 50.0 else 25.0
        reasons += if (counted) "Point counts in front of the axes match them" else "Rising axis points with no counts"
        val points = axes.sumOf { it.points.size }
        score += minOf(15.0, points * 0.6)
        val distinct = values.toSet().size
        val fill = values.count { it == type.max || it == 0.0 && !type.signed } / values.size.toDouble()
        when {
            distinct == 1 -> { score -= 30; reasons += "Every value is the same" }
            else -> {
                val smoothPart = ((0.3 - smooth) / 0.3).coerceIn(0.0, 1.0) * 35
                score += smoothPart
                reasons += when {
                    smooth < 0.1 -> "The values change smoothly"
                    smooth < 0.2 -> "The values are fairly smooth"
                    else -> "The values jump around"
                }
            }
        }
        if (fill > 0.5) { score -= 20; reasons += "Over half the values are 0 or the highest value (erased or unused)" }
        // Words bunched at both ends of their unsigned range are signed numbers read unsigned
        if (!type.signed && !type.float && type.size >= 2) {
            val low = values.count { it < type.max * 0.1 }
            val high = values.count { it > type.max * 0.9 }
            if (low > 0 && high > 0 && low + high > values.size / 10) {
                score -= 25
                reasons += "Values sit at both ends of the type, as negative numbers read unsigned do"
            }
        }
        // Axes that rise evenly or gently are typical; ones with wild steps less so
        val spread = axes.map { axis -> axis.points.last() - axis.points.first() }
        if (spread.any { it > 0 && it < 2.0 }) score -= 10
        return score.toInt().coerceIn(0, 100) to reasons
    }

    private fun align(address: Long, size: Int): Long = if (size <= 1) address else (address + size - 1) / size * size
}
