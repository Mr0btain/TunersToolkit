package org.mr0btain.tunerstoolkit.core.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryImageTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun sample() = MemoryImage.of(
        0x1000L to bytes(0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0),
        0x8000_0000L to bytes(0xFF, 0xFE, 0x80, 0x00)
    )

    @Test
    fun layout() {
        val image = sample()
        assertEquals(listOf(AddressRange(0x1000, 0x1008), AddressRange(0x8000_0000L, 0x8000_0004L)), image.ranges)
        assertEquals(12, image.size)
        assertEquals(AddressRange(0x1000, 0x8000_0004L), image.span)
        assertTrue(image.contains(0x1000, 8))
        assertFalse(image.contains(0x1000, 9))
        assertFalse(image.contains(0x0FFF))
        assertEquals(AddressRange(0x8000_0000L, 0x8000_0004L), image.rangeAt(0x8000_0003L))
        assertNull(image.rangeAt(0x2000))
    }

    @Test
    fun readingBytes() {
        val image = sample()
        assertEquals(0x12, image.byteAt(0x1000))
        assertEquals(0xF0, image.byteAt(0x1007))
        assertNull(image.byteAtOrNull(0x1008))
        assertContentEquals(bytes(0x56, 0x78), image.read(0x1002, 2))
        assertNull(image.readOrNull(0x1006, 4))
        assertFailsWith<IndexOutOfBoundsException> { image.read(0x7FFF_FFFFL, 2) }
    }

    @Test
    fun readingAcrossAGapFillsIt() {
        val image = MemoryImage.of(0x10L to bytes(1, 2), 0x14L to bytes(3, 4))
        val out = ByteArray(8) { 0x55 }
        val present = BooleanArray(8)
        image.readInto(0x0F, 8, out, present)
        assertContentEquals(bytes(0, 1, 2, 0, 0, 3, 4, 0), out)
        assertContentEquals(booleanArrayOf(false, true, true, false, false, true, true, false), present)
    }

    @Test
    fun typedReadsInBothByteOrders() {
        val image = sample()
        assertEquals(0x1234UL, image.readUnsigned(0x1000, 2, ByteOrder.BIG_ENDIAN))
        assertEquals(0x3412UL, image.readUnsigned(0x1000, 2, ByteOrder.LITTLE_ENDIAN))
        assertEquals(0x12345678UL, image.readUnsigned(0x1000, 4, ByteOrder.BIG_ENDIAN))
        assertEquals(0xF0DEBC9A78563412UL, image.readUnsigned(0x1000, 8, ByteOrder.LITTLE_ENDIAN))

        assertEquals(-1, image.readSigned(0x8000_0000L, 1, ByteOrder.BIG_ENDIAN))
        assertEquals(-2, image.readSigned(0x8000_0000L, 2, ByteOrder.BIG_ENDIAN))
        assertEquals(-32768, image.readSigned(0x8000_0002L, 2, ByteOrder.BIG_ENDIAN))
        assertEquals(0x80, image.readSigned(0x8000_0002L, 2, ByteOrder.LITTLE_ENDIAN))
        assertEquals(0xF0DEBC9A78563412UL.toLong(), image.readSigned(0x1000, 8, ByteOrder.LITTLE_ENDIAN))
    }

    @Test
    fun floats() {
        val image = MemoryImage.of(0L to ByteArray(16))
        image.writeFloat32(0, 1.5f, ByteOrder.BIG_ENDIAN)
        assertContentEquals(bytes(0x3F, 0xC0, 0x00, 0x00), image.read(0, 4))
        assertEquals(1.5f, image.readFloat32(0, ByteOrder.BIG_ENDIAN))

        image.writeFloat64(8, -2.25, ByteOrder.LITTLE_ENDIAN)
        assertEquals(-2.25, image.readFloat64(8, ByteOrder.LITTLE_ENDIAN))
        assertEquals(0xC0.toByte(), image.read(15, 1)[0])
    }

    @Test
    fun typedWritesCheckTheValueFits() {
        val image = MemoryImage.of(0L to ByteArray(8))
        image.writeUnsigned(0, 2, 0xBEEFUL, ByteOrder.LITTLE_ENDIAN)
        assertContentEquals(bytes(0xEF, 0xBE), image.read(0, 2))
        image.writeSigned(2, 2, -2, ByteOrder.BIG_ENDIAN)
        assertContentEquals(bytes(0xFF, 0xFE), image.read(2, 2))

        assertFailsWith<IllegalArgumentException> { image.writeUnsigned(0, 1, 256UL, ByteOrder.BIG_ENDIAN) }
        assertFailsWith<IllegalArgumentException> { image.writeSigned(0, 1, 128, ByteOrder.BIG_ENDIAN) }
        assertFailsWith<IllegalArgumentException> { image.writeSigned(0, 1, -129, ByteOrder.BIG_ENDIAN) }
        assertFailsWith<IndexOutOfBoundsException> { image.write(7, bytes(1, 2)) }
    }

    @Test
    fun writesAreTrackedAgainstTheOriginal() {
        val image = sample()
        val versionBefore = image.version
        assertFalse(image.write(0x1000, bytes(0x12)), "Writing the same byte changes nothing")
        assertEquals(versionBefore, image.version)

        assertTrue(image.write(0x1002, bytes(0x00, 0x00)))
        assertTrue(image.write(0x8000_0003L, bytes(0x42)))
        assertTrue(image.isChanged(0x1002))
        assertFalse(image.isChanged(0x1004))
        assertEquals(listOf(AddressRange(0x1002, 0x1004), AddressRange(0x8000_0003L, 0x8000_0004L)), image.changedRanges())

        val patches = image.patchesFromOriginal()
        assertEquals(2, patches.size)
        assertContentEquals(bytes(0x56, 0x78), patches[0].before)
        assertContentEquals(bytes(0x00, 0x00), patches[0].after)

        // The original plus the patches is the image as it is now
        val rebuilt = image.original().also { it.apply(patches) }
        assertTrue(rebuilt.contentEquals(image))
        assertTrue(image.original().contentEquals(sample()))
    }

    @Test
    fun undoAndRedo() {
        val image = sample()
        image.write(0x1000, bytes(0xAA))
        image.edit {
            write(0x1001, bytes(0xBB))
            write(0x1002, bytes(0xCC))
        }
        assertEquals(0xCC, image.byteAt(0x1002))

        assertTrue(image.undo())
        assertEquals(0x34, image.byteAt(0x1001), "The edit is undone as one step")
        assertEquals(0x56, image.byteAt(0x1002))
        assertEquals(0xAA, image.byteAt(0x1000))

        assertTrue(image.undo())
        assertEquals(0x12, image.byteAt(0x1000))
        assertFalse(image.canUndo)
        assertFalse(image.undo())
        assertTrue(image.changedRanges().isEmpty())

        assertTrue(image.redo())
        assertTrue(image.redo())
        assertEquals(0xCC, image.byteAt(0x1002))
        assertFalse(image.canRedo)

        // A new write after undoing drops what could be redone
        image.undo()
        image.write(0x1007, bytes(0x01))
        assertFalse(image.canRedo)
    }

    @Test
    fun undoKeepsOnlyTheNewestSteps() {
        val image = MemoryImage.of(0L to ByteArray(10))
        image.maxUndoSteps = 3
        repeat(10) { image.write(it.toLong(), bytes(1)) }
        repeat(3) { assertTrue(image.undo()) }
        assertFalse(image.undo())
        assertEquals(1, image.byteAt(6))
        assertEquals(0, image.byteAt(7))
    }

    @Test
    fun snapshotForgetsHistory() {
        val image = sample()
        image.write(0x1000, bytes(0x00))
        val snapshot = image.snapshot()
        assertTrue(snapshot.contentEquals(image))
        assertTrue(snapshot.changedRanges().isEmpty())
        assertFalse(snapshot.canUndo)
    }

    @Test
    fun diffFindsChangedAndMissingBytes() {
        val a = MemoryImage.of(0x10L to bytes(1, 2, 3, 4), 0x20L to bytes(9))
        val b = MemoryImage.of(0x10L to bytes(1, 0, 0, 4, 5))
        assertEquals(
            listOf(AddressRange(0x11, 0x13), AddressRange(0x14, 0x15), AddressRange(0x20, 0x21)),
            a.diff(b)
        )
        assertEquals(a.diff(b), b.diff(a))
        assertTrue(a.diff(a.snapshot()).isEmpty())
    }

    @Test
    fun builderJoinsTouchingPiecesAndReportsOverlaps() {
        val built = MemoryImage.Builder()
            .put(0x20, bytes(5, 6))
            .put(0x10, bytes(1, 2))
            .put(0x12, bytes(3, 4))      // touches the first, becomes one segment
            .put(0x13, bytes(0xAA, 0xBB)) // overlaps 0x13, later bytes win
            .build()

        assertEquals(listOf(AddressRange(0x10, 0x15), AddressRange(0x20, 0x22)), built.image.ranges)
        assertContentEquals(bytes(1, 2, 3, 0xAA, 0xBB), built.image.read(0x10, 5))
        assertEquals(listOf(AddressRange(0x13, 0x14)), built.overlaps)
    }

    @Test
    fun builderKeepsFileOrderForPiecesAtTheSameAddress() {
        val built = MemoryImage.Builder().put(0x10, bytes(1)).put(0x40, bytes(2)).put(0x10, bytes(3)).build()
        assertEquals(3, built.image.byteAt(0x10))
        assertEquals(listOf(AddressRange(0x10, 0x11)), built.overlaps)
    }

    @Test
    fun emptyImage() {
        val image = MemoryImage.empty()
        assertEquals(0, image.size)
        assertNull(image.span)
        assertFalse(image.contains(0))
    }

    @Test
    fun rangesPrintAsHex() {
        assertEquals("0x80000000-0x8000FFFF", AddressRange(0x8000_0000L, 0x8001_0000L).toString())
        assertEquals(
            listOf(AddressRange(0, 10), AddressRange(11, 12)),
            listOf(AddressRange(5, 10), AddressRange(0, 6), AddressRange(11, 12)).merged()
        )
    }
}
