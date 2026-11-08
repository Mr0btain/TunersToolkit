package org.mr0btain.tunerstoolkit.ui.kit.hex

import org.mr0btain.tunerstoolkit.core.image.AddressRange
import kotlin.test.Test
import kotlin.test.assertEquals

class HexRowsTest {
    private val data = HexRows.Row::Data

    @Test
    fun oneAlignedSegment() {
        val rows = HexRows(listOf(AddressRange(0x100, 0x140)), 16)
        assertEquals(4, rows.count)
        assertEquals(data(0x100), rows.rowAt(0))
        assertEquals(data(0x130), rows.rowAt(3))
        assertEquals(2, rows.rowOf(0x12F))
        assertEquals(0, rows.rowOf(0))
        assertEquals(3, rows.rowOf(0xFFFF))
    }

    @Test
    fun unalignedSegmentsShareRows() {
        // 0x105-0x112 and 0x118-0x121 both touch the rows at 0x100, 0x110 and 0x120
        val rows = HexRows(listOf(AddressRange(0x105, 0x112), AddressRange(0x118, 0x121)), 16)
        assertEquals(3, rows.count)
        assertEquals(data(0x120), rows.rowAt(2))
        assertEquals(1, rows.rowOf(0x112), "In the small gap inside a block it's still that row")
    }

    @Test
    fun farApartSegmentsGetAGapRow() {
        val rows = HexRows(listOf(AddressRange(0x8000_0000L, 0x8000_0020L), AddressRange(0xA000_0000L, 0xA000_0010L)), 16)
        assertEquals(4, rows.count)
        assertEquals(data(0x8000_0010L), rows.rowAt(1))
        assertEquals(HexRows.Row.Gap(AddressRange(0x8000_0020L, 0xA000_0000L)), rows.rowAt(2))
        assertEquals(data(0xA000_0000L), rows.rowAt(3))
        assertEquals(2, rows.rowOf(0x9000_0000L))
        assertEquals(3, rows.rowOf(0xA000_000FL))
    }

    @Test
    fun manySegmentsRoundTrip() {
        val ranges = (0 until 50).map { AddressRange.ofSize(0x1000L * it * it + it * 3, 37L + it) }
        val rows = HexRows(ranges, 32)
        for (index in 0 until rows.count) {
            val row = rows.rowAt(index)
            val address = when (row) {
                is HexRows.Row.Data -> row.address
                is HexRows.Row.Gap -> row.range.start
            }
            assertEquals(index, rows.rowOf(address), "Row $index")
        }
    }

    @Test
    fun empty() {
        val rows = HexRows(emptyList(), 16)
        assertEquals(0, rows.count)
        assertEquals(0, rows.rowOf(0x1234))
    }
}
