package org.mr0btain.tunerstoolkit.core.image

/**
 * One change to the image: the bytes at [address] went from [before] to [after]
 */
class Patch(val address: Long, before: ByteArray, after: ByteArray) {
    private val beforeBytes = before.copyOf()
    private val afterBytes = after.copyOf()

    init {
        require(before.size == after.size) { "A patch changes bytes in place, before and after must be the same size" }
    }

    val before: ByteArray get() = beforeBytes.copyOf()
    val after: ByteArray get() = afterBytes.copyOf()
    val range: AddressRange get() = AddressRange.ofSize(address, afterBytes.size.toLong())

    /** The same change undone */
    fun reversed(): Patch = Patch(address, afterBytes, beforeBytes)

    override fun equals(other: Any?): Boolean = other is Patch && address == other.address &&
        beforeBytes.contentEquals(other.beforeBytes) && afterBytes.contentEquals(other.afterBytes)

    override fun hashCode(): Int = (address.hashCode() * 31 + beforeBytes.contentHashCode()) * 31 + afterBytes.contentHashCode()

    override fun toString(): String = "Patch($range, ${afterBytes.size} bytes)"
}

/**
 * The bytes of an ECU image: one or more segments at their addresses, with gaps between them. This is the one place
 * binary data lives; hex text is only ever made for what's on screen.
 *
 * Every write is kept as a [Patch], so it can be undone, and [changedRanges] / [patchesFromOriginal] compare the
 * image with what it was when loaded. Writes inside [edit] are undone together.
 *
 * Not safe to change from several threads at once.
 * @property startAddress Where execution starts, from the file's start record, if it had one
 * @property header The S-record S0 header text, if the file had one
 */
class MemoryImage internal constructor(
    private val starts: LongArray,
    private val current: Array<ByteArray>,
    val startAddress: Long? = null,
    val header: String? = null
) {
    private val original: Array<ByteArray> = Array(current.size) { current[it].copyOf() }
    private val undoSteps = ArrayDeque<List<Patch>>()
    private val redoSteps = ArrayDeque<List<Patch>>()
    private var pending: MutableList<Patch>? = null

    init {
        for (i in 1 until starts.size) {
            require(starts[i] > starts[i - 1] + current[i - 1].size) { "Segments must be sorted, apart and not touching" }
        }
    }

    /** Where each segment is, in address order */
    val ranges: List<AddressRange> = starts.indices.map { AddressRange.ofSize(starts[it], current[it].size.toLong()) }

    /** All the bytes in the image, gaps not counted */
    val size: Long = current.sumOf { it.size.toLong() }

    /** The span from the first segment's start to the last one's end, gaps included. Null when empty */
    val span: AddressRange? = if (starts.isEmpty()) null else AddressRange(starts.first(), ranges.last().endExclusive)

    /** Counts up with every change, undo and redo, so a screen can tell when to redraw */
    var version: Long = 0
        private set

    /** Steps that can be undone, newest last. Most [maxUndoSteps] are kept */
    var maxUndoSteps: Int = 1000

    val canUndo: Boolean get() = undoSteps.isNotEmpty()
    val canRedo: Boolean get() = redoSteps.isNotEmpty()

    // ------------------------------------------- Reading -------------------------------------------

    /** Whether all of [address] to [address] + [length] is inside one segment */
    fun contains(address: Long, length: Int = 1): Boolean = locate(address, length) >= 0

    /** The range holding [address], or null in a gap */
    fun rangeAt(address: Long): AddressRange? = segmentIndex(address).takeIf { it >= 0 }?.let { ranges[it] }

    /** The byte at [address] as 0 to 255, or null in a gap */
    fun byteAtOrNull(address: Long): Int? {
        val index = segmentIndex(address)
        if (index < 0) return null
        return current[index][(address - starts[index]).toInt()].toInt() and 0xFF
    }

    fun byteAt(address: Long): Int = byteAtOrNull(address) ?: throw outOfRange(address, 1)

    /** [length] bytes from [address], or null if any of them is in a gap */
    fun readOrNull(address: Long, length: Int): ByteArray? {
        val index = locate(address, length)
        if (index < 0) return null
        val offset = (address - starts[index]).toInt()
        return current[index].copyOfRange(offset, offset + length)
    }

    fun read(address: Long, length: Int): ByteArray = readOrNull(address, length) ?: throw outOfRange(address, length)

    /**
     * Copies what's there of [address] to [address] + [length] into [destination], for drawing rows that may run
     * into a gap
     * @return For each byte, whether it was there
     */
    fun readInto(address: Long, length: Int, destination: ByteArray, present: BooleanArray? = null) {
        require(destination.size >= length) { "The destination is too small" }
        var position = address
        var written = 0
        while (written < length) {
            val index = segmentIndex(position)
            if (index < 0) {
                // In a gap: up to the next segment, or the end
                val next = nextSegmentAfter(position)
                val gapEnd = if (next < 0) address + length else minOf(starts[next], address + length)
                val count = (gapEnd - position).toInt()
                destination.fill(0, written, written + count)
                present?.fill(false, written, written + count)
                written += count
                position = gapEnd
            } else {
                val offset = (position - starts[index]).toInt()
                val count = minOf(length - written, current[index].size - offset)
                current[index].copyInto(destination, written, offset, offset + count)
                present?.fill(true, written, written + count)
                written += count
                position += count
            }
        }
    }

    /** The unsigned value of [size] (1 to 8) bytes at [address] */
    fun readUnsigned(address: Long, size: Int, order: ByteOrder): ULong = order.decode(read(address, size), 0, size)

    /** The signed (two's complement) value of [size] (1 to 8) bytes at [address] */
    fun readSigned(address: Long, size: Int, order: ByteOrder): Long {
        val shift = 64 - size * 8
        return (readUnsigned(address, size, order).toLong() shl shift) shr shift
    }

    fun readFloat32(address: Long, order: ByteOrder): Float = Float.fromBits(readUnsigned(address, 4, order).toInt())

    fun readFloat64(address: Long, order: ByteOrder): Double = Double.fromBits(readUnsigned(address, 8, order).toLong())

    // ------------------------------------------- Writing -------------------------------------------

    /**
     * Writes [bytes] at [address]. They have to fit inside one segment: an image's layout never changes once loaded
     * @return False when nothing changed, the bytes were already there
     */
    fun write(address: Long, bytes: ByteArray): Boolean {
        val index = locate(address, bytes.size)
        if (index < 0) throw outOfRange(address, bytes.size)
        val offset = (address - starts[index]).toInt()
        val segment = current[index]
        val before = segment.copyOfRange(offset, offset + bytes.size)
        if (before.contentEquals(bytes)) return false

        bytes.copyInto(segment, offset)
        record(Patch(address, before, bytes))
        version++
        return true
    }

    fun writeUnsigned(address: Long, size: Int, value: ULong, order: ByteOrder): Boolean {
        require(size == 8 || value < (1UL shl (size * 8))) { "$value doesn't fit in $size bytes" }
        return write(address, order.encode(value, size))
    }

    fun writeSigned(address: Long, size: Int, value: Long, order: ByteOrder): Boolean {
        if (size < 8) {
            val limit = 1L shl (size * 8 - 1)
            require(value in -limit until limit) { "$value doesn't fit in $size signed bytes" }
        }
        return write(address, order.encode(value.toULong(), size))
    }

    fun writeFloat32(address: Long, value: Float, order: ByteOrder): Boolean =
        writeUnsigned(address, 4, value.toRawBits().toUInt().toULong(), order)

    fun writeFloat64(address: Long, value: Double, order: ByteOrder): Boolean =
        writeUnsigned(address, 8, value.toRawBits().toULong(), order)

    /** Runs [block], and its writes are undone and redone together as one step */
    fun <T> edit(block: MemoryImage.() -> T): T {
        if (pending != null) return block()
        val step = mutableListOf<Patch>()
        pending = step
        try {
            return block()
        } finally {
            pending = null
            if (step.isNotEmpty()) pushUndo(step)
        }
    }

    /** Writes each patch's after bytes, as one step */
    fun apply(patches: List<Patch>) = edit { patches.forEach { write(it.address, it.after) } }

    // ------------------------------------------- Undo -------------------------------------------

    fun undo(): Boolean {
        check(pending == null) { "Can't undo inside edit" }
        val step = undoSteps.removeLastOrNull() ?: return false
        step.asReversed().forEach { put(it.address, it.before) }
        redoSteps.addLast(step)
        version++
        return true
    }

    fun redo(): Boolean {
        check(pending == null) { "Can't redo inside edit" }
        val step = redoSteps.removeLastOrNull() ?: return false
        step.forEach { put(it.address, it.after) }
        undoSteps.addLast(step)
        version++
        return true
    }

    // ------------------------------------------- Changes -------------------------------------------

    /** Whether the byte at [address] differs from when the image was loaded */
    fun isChanged(address: Long): Boolean {
        val index = segmentIndex(address)
        if (index < 0) return false
        val offset = (address - starts[index]).toInt()
        return current[index][offset] != original[index][offset]
    }

    /** Where the bytes differ from when the image was loaded */
    fun changedRanges(): List<AddressRange> = changedRuns().map { (index, from, to) ->
        AddressRange(starts[index] + from, starts[index] + to)
    }

    /** The changes since the image was loaded, as few patches as possible. Applied to the original, they give this */
    fun patchesFromOriginal(): List<Patch> = changedRuns().map { (index, from, to) ->
        Patch(starts[index] + from, original[index].copyOfRange(from, to), current[index].copyOfRange(from, to))
    }

    /** The bytes as they were when the image was loaded */
    fun original(): MemoryImage = MemoryImage(starts.copyOf(), Array(original.size) { original[it].copyOf() }, startAddress, header)

    /** This image as it is now, with no history: its original is these bytes */
    fun snapshot(): MemoryImage = MemoryImage(starts.copyOf(), Array(current.size) { current[it].copyOf() }, startAddress, header)

    /** The bytes of segment [index], a copy */
    fun segmentBytes(index: Int): ByteArray = current[index].copyOf()

    /** Same segments with the same bytes, whatever the history */
    fun contentEquals(other: MemoryImage): Boolean =
        starts.contentEquals(other.starts) && current.indices.all { current[it].contentEquals(other.current[it]) }

    /**
     * Where this and [other] differ: bytes that aren't the same, and bytes only one of them has
     */
    fun diff(other: MemoryImage): List<AddressRange> {
        val runs = mutableListOf<AddressRange>()
        var runStart = -1L
        var runEnd = -1L

        fun mark(address: Long) {
            if (runStart >= 0 && address == runEnd) {
                runEnd++
            } else {
                if (runStart >= 0) runs += AddressRange(runStart, runEnd)
                runStart = address
                runEnd = address + 1
            }
        }

        for (index in starts.indices) {
            val bytes = current[index]
            for (offset in bytes.indices) {
                val address = starts[index] + offset
                val theirs = other.byteAtOrNull(address)
                if (theirs == null || theirs != (bytes[offset].toInt() and 0xFF)) mark(address)
            }
        }
        for (index in other.starts.indices) {
            for (offset in other.current[index].indices) {
                val address = other.starts[index] + offset
                if (segmentIndex(address) < 0) mark(address)
            }
        }
        if (runStart >= 0) runs += AddressRange(runStart, runEnd)
        return runs.merged()
    }

    override fun toString(): String = "MemoryImage(${ranges.joinToString()}, $size bytes)"

    // ------------------------------------------- Inside -------------------------------------------

    /** The segment holding [address], or -1 */
    private fun segmentIndex(address: Long): Int {
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            when {
                address < starts[mid] -> high = mid - 1
                address >= starts[mid] + current[mid].size -> low = mid + 1
                else -> return mid
            }
        }
        return -1
    }

    /** The first segment starting after [address], or -1 */
    private fun nextSegmentAfter(address: Long): Int {
        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (starts[mid] <= address) low = mid + 1 else high = mid
        }
        return if (low < starts.size) low else -1
    }

    /** The segment holding all of [address] to [address] + [length], or -1 */
    private fun locate(address: Long, length: Int): Int {
        if (length < 0) return -1
        val index = segmentIndex(address)
        if (index < 0) return -1
        return if (address + length <= starts[index] + current[index].size) index else -1
    }

    private fun put(address: Long, bytes: ByteArray) {
        val index = locate(address, bytes.size)
        bytes.copyInto(current[index], (address - starts[index]).toInt())
    }

    private fun record(patch: Patch) {
        val step = pending
        if (step != null) step += patch else pushUndo(listOf(patch))
    }

    private fun pushUndo(step: List<Patch>) {
        undoSteps.addLast(step)
        while (undoSteps.size > maxUndoSteps) undoSteps.removeFirst()
        redoSteps.clear()
    }

    /** Runs of changed bytes as (segment, from, to) */
    private fun changedRuns(): List<Triple<Int, Int, Int>> {
        val runs = mutableListOf<Triple<Int, Int, Int>>()
        for (index in current.indices) {
            val now = current[index]
            val was = original[index]
            var offset = 0
            while (offset < now.size) {
                if (now[offset] != was[offset]) {
                    val from = offset
                    while (offset < now.size && now[offset] != was[offset]) offset++
                    runs += Triple(index, from, offset)
                } else {
                    offset++
                }
            }
        }
        return runs
    }

    private fun outOfRange(address: Long, length: Int) = IndexOutOfBoundsException(
        "${AddressRange.ofSize(address, length.toLong().coerceAtLeast(1))} isn't all inside one segment of $this"
    )

    /**
     * Puts an image together from bytes at addresses, in any order. Touching pieces become one segment.
     * Overlapping pieces are kept, the later one's bytes winning, and listed in [Built.overlaps]
     */
    class Builder {
        private class Piece(val start: Long, val order: Int, val bytes: GrowableBytes)

        private val pieces = mutableListOf<Piece>()
        private var last: Piece? = null

        var startAddress: Long? = null
        var header: String? = null

        /** Whether anything was put in */
        val isEmpty: Boolean get() = pieces.isEmpty()

        fun put(address: Long, bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Builder {
            require(address >= 0) { "Address ${address.toHexAddress()} is negative" }
            if (length == 0) return this
            val piece = last
            if (piece != null && piece.start + piece.bytes.size == address) {
                piece.bytes.add(bytes, offset, length)
            } else {
                val next = Piece(address, pieces.size, GrowableBytes(maxOf(length, 4096)))
                next.bytes.add(bytes, offset, length)
                pieces += next
                last = next
            }
            return this
        }

        fun build(): Built {
            val overlaps = mutableListOf<AddressRange>()
            val starts = mutableListOf<Long>()
            val merged = mutableListOf<GrowableBytes>()

            for (piece in pieces.sortedWith(compareBy({ it.start }, { it.order }))) {
                val bytes = piece.bytes.toByteArray()
                val end = piece.start + bytes.size
                val lastStart = starts.lastOrNull()
                val lastBytes = merged.lastOrNull()
                if (lastStart != null && lastBytes != null && piece.start <= lastStart + lastBytes.size) {
                    val lastEnd = lastStart + lastBytes.size
                    if (piece.start < lastEnd) overlaps += AddressRange(piece.start, minOf(lastEnd, end))
                    lastBytes.set((piece.start - lastStart).toInt(), bytes)
                } else {
                    starts += piece.start
                    merged += GrowableBytes(bytes.size).also { it.add(bytes) }
                }
            }

            val image = MemoryImage(
                starts = starts.toLongArray(),
                current = Array(merged.size) { merged[it].toByteArray() },
                startAddress = startAddress,
                header = header
            )
            return Built(image, overlaps.merged())
        }
    }

    /** What [Builder.build] made, and where pieces overlapped */
    data class Built(val image: MemoryImage, val overlaps: List<AddressRange>)

    companion object {
        /** An image with nothing in it */
        fun empty(): MemoryImage = MemoryImage(LongArray(0), emptyArray())

        /** An image of the given segments, which must not overlap */
        fun of(vararg segments: Pair<Long, ByteArray>): MemoryImage {
            val built = Builder().apply { segments.forEach { (address, bytes) -> put(address, bytes) } }.build()
            require(built.overlaps.isEmpty()) { "Segments overlap at ${built.overlaps.joinToString()}" }
            return built.image
        }
    }
}
