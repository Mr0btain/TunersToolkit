package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.merged

/** One part of one object, as the index holds it */
class IndexedPart(val obj: CalibrationObject, val part: ObjectPart) {
    val range: AddressRange get() = part.range
    override fun toString(): String = "${obj.name} $part"
}

/** Two objects sharing memory, which a broken definition can do */
class ObjectOverlap(val first: String, val second: String, val range: AddressRange)

/**
 * Which objects are where: every object's parts sorted by address, so the bytes on screen can ask what's in them in
 * O(log n + k). Parts can overlap and nest, so alongside the starts it keeps the furthest end seen so far, which
 * tells a lookup when it can stop walking back
 */
class ObjectIndex private constructor(private val entries: List<IndexedPart>) {
    private val starts = LongArray(entries.size) { entries[it].range.start }

    /** The furthest end among entries 0..i */
    private val reach = LongArray(entries.size).also { reach ->
        var furthest = Long.MIN_VALUE
        entries.forEachIndexed { i, entry ->
            furthest = maxOf(furthest, entry.range.endExclusive)
            reach[i] = furthest
        }
    }

    private val byName: Map<String, List<ObjectPart>> = entries.groupBy({ it.obj.name }, { it.part })

    val size: Int get() = entries.size

    /** Every object with a part in the index, in address order */
    val objects: List<CalibrationObject> get() = entries.map { it.obj }.distinctBy { it.name }

    /** All memory any object takes, merged, for a minimap */
    val coverage: List<AddressRange> by lazy { entries.map { it.range }.merged() }

    /** Every part overlapping [range], in address order */
    fun overlapping(range: AddressRange): List<IndexedPart> {
        if (entries.isEmpty() || range.size <= 0) return emptyList()
        // The last entry starting before the range ends
        var low = 0
        var high = entries.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (starts[middle] < range.endExclusive) low = middle + 1 else high = middle
        }
        val found = ArrayList<IndexedPart>()
        var i = low - 1
        while (i >= 0 && reach[i] > range.start) {
            if (entries[i].range.endExclusive > range.start) found += entries[i]
            i--
        }
        found.reverse()
        return found
    }

    /** The parts holding the byte at [address] */
    fun at(address: Long): List<IndexedPart> = overlapping(AddressRange.ofSize(address, 1))

    /** An object's own parts */
    fun parts(name: String): List<ObjectPart> = byName[name].orEmpty()

    /**
     * Places where different objects share memory, at most [limit] of them. Parts of one object (and an object
     * nested in itself) don't count
     */
    fun overlaps(limit: Int = 1000): List<ObjectOverlap> {
        val found = mutableListOf<ObjectOverlap>()
        for (i in entries.indices) {
            val a = entries[i]
            var j = i + 1
            while (j < entries.size && starts[j] < a.range.endExclusive) {
                val b = entries[j]
                if (b.obj.name != a.obj.name) {
                    val start = maxOf(a.range.start, b.range.start)
                    val end = minOf(a.range.endExclusive, b.range.endExclusive)
                    found += ObjectOverlap(a.obj.name, b.obj.name, AddressRange(start, end))
                    if (found.size >= limit) return found
                }
                j++
            }
        }
        return found
    }

    companion object {
        /** An index of parts already known */
        fun of(parts: List<IndexedPart>): ObjectIndex = ObjectIndex(parts.sortedWith(compareBy({ it.range.start }, { it.range.endExclusive })))

        /**
         * Reads every object of [set] from [image] to find where its parts are. Objects that can't be read are
         * left out, each with an error saying why
         */
        fun build(set: CalibrationSet, image: MemoryImage): IndexBuild {
            val parts = ArrayList<IndexedPart>()
            val problems = mutableListOf<Diagnostic>()
            var decoded = 0
            for (obj in set.objects) {
                when (val result = set.decode(image, obj)) {
                    is TkResult.Success -> {
                        decoded++
                        result.data.parts.forEach { parts += IndexedPart(obj, it) }
                        problems += result.data.warnings
                    }
                    is TkResult.Failure -> problems += Diagnostic(Severity.ERROR, result.error.message, obj.source, obj.name)
                }
            }
            return IndexBuild(of(parts), decoded, problems)
        }
    }
}

/**
 * An index and what building it found
 * @property decoded How many objects could be read
 * @property problems Objects that couldn't be read (errors) and ones that read oddly (warnings)
 */
class IndexBuild(val index: ObjectIndex, val decoded: Int, val problems: List<Diagnostic>)
