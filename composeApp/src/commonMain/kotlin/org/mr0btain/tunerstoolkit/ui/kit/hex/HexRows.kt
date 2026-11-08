package org.mr0btain.tunerstoolkit.ui.kit.hex

import org.mr0btain.tunerstoolkit.core.image.AddressRange

/**
 * The hex view's rows. Each run of segments becomes a block of rows of [bytesPerRow] bytes, aligned so a row always
 * starts at a multiple of [bytesPerRow] (segments close enough to share a row share a block). Between blocks there's
 * one gap row standing for everything skipped. Nothing is made per row: looking a row or an address up is a binary
 * search over the blocks, so a 4MB image (262,144 rows of 16) costs nothing to scroll or jump through
 */
class HexRows(ranges: List<AddressRange>, val bytesPerRow: Int) {
    sealed interface Row {
        /** Bytes from [address] for one row; some may be in a gap if a segment starts or ends partway along */
        data class Data(val address: Long) : Row

        /** The addresses skipped between two blocks */
        data class Gap(val range: AddressRange) : Row
    }

    private val blockStart: LongArray
    private val blockEnd: LongArray
    private val blockFirstRow: IntArray

    /** How many rows there are, gap rows included */
    val count: Int

    init {
        require(bytesPerRow > 0) { "Rows hold at least one byte" }
        val starts = mutableListOf<Long>()
        val ends = mutableListOf<Long>()
        for (range in ranges.sorted()) {
            if (range.isEmpty) continue
            val start = range.start - range.start % bytesPerRow
            val end = (range.endExclusive + bytesPerRow - 1) / bytesPerRow * bytesPerRow
            if (ends.isNotEmpty() && start <= ends.last()) {
                ends[ends.lastIndex] = maxOf(ends.last(), end)
            } else {
                starts += start
                ends += end
            }
        }
        blockStart = starts.toLongArray()
        blockEnd = ends.toLongArray()
        blockFirstRow = IntArray(starts.size)
        var row = 0L
        for (i in starts.indices) {
            blockFirstRow[i] = row.toInt()
            row += (ends[i] - starts[i]) / bytesPerRow
            if (i < starts.lastIndex) row++ // the gap row after it
        }
        require(row <= Int.MAX_VALUE) { "Too many rows to show" }
        count = row.toInt()
    }

    fun rowAt(index: Int): Row {
        require(index in 0 until count) { "Row $index isn't one of $count" }
        val block = blockAtRow(index)
        val offset = index - blockFirstRow[block]
        val rows = ((blockEnd[block] - blockStart[block]) / bytesPerRow).toInt()
        return if (offset < rows) Row.Data(blockStart[block] + offset.toLong() * bytesPerRow)
        else Row.Gap(AddressRange(blockEnd[block], blockStart[block + 1]))
    }

    /**
     * The row showing [address]: its data row, the gap row when it's in a gap, the first row before everything and
     * the last after
     */
    fun rowOf(address: Long): Int {
        if (count == 0) return 0
        if (address < blockStart[0]) return 0
        var low = 0
        var high = blockStart.size - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (blockStart[mid] <= address) low = mid else high = mid - 1
        }
        val block = low
        return if (address < blockEnd[block]) {
            blockFirstRow[block] + ((address - blockStart[block]) / bytesPerRow).toInt()
        } else if (block < blockStart.lastIndex) {
            blockFirstRow[block + 1] - 1
        } else {
            count - 1
        }
    }

    private fun blockAtRow(index: Int): Int {
        var low = 0
        var high = blockFirstRow.size - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (blockFirstRow[mid] <= index) low = mid else high = mid - 1
        }
        return low
    }
}
