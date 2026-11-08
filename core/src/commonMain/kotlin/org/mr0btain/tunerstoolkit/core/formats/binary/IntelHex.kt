package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostics
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/**
 * Intel HEX: `:LLAAAATT<data>CC` lines. Record types:
 * - 00 data, at the current base plus the record's 16 bit address
 * - 01 end of file
 * - 02 extended segment address: base = value × 16
 * - 03 start segment address (CS:IP), kept as [MemoryImage.startAddress] (CS × 16 + IP)
 * - 04 extended linear address: base = value shl 16
 * - 05 start linear address, kept as [MemoryImage.startAddress]
 *
 * Reading never throws: a broken record is left out and reported, and the rest still loads.
 */
object IntelHex {
    private const val DATA = 0x00
    private const val END_OF_FILE = 0x01
    private const val EXTENDED_SEGMENT = 0x02
    private const val START_SEGMENT = 0x03
    private const val EXTENDED_LINEAR = 0x04
    private const val START_LINEAR = 0x05

    /** Most problems reported one by one; past this, they're counted in one summary line */
    private const val MAX_REPORTED = 200

    fun read(text: ByteArray, fileName: String = "file.hex"): TkResult<Imported<MemoryImage>, ImportError> {
        if (HexText.firstContentIndex(text) < 0) return TkResult.Failure(ImportError.EMPTY_FILE)

        val diagnostics = Diagnostics()
        val builder = MemoryImage.Builder()
        var base = 0L
        var endSeen = false
        var afterEnd = 0
        var reported = 0
        var unreported = 0
        val record = ByteArray(255 + 5)

        fun problem(error: Boolean, line: Int, column: Int, message: String) {
            if (reported >= MAX_REPORTED) {
                unreported++
                return
            }
            reported++
            val location = SourceLocation(fileName, line, column)
            if (error) diagnostics.error(message, location) else diagnostics.warning(message, location)
        }

        HexText.forEachLine(text) { lineStart, lineEnd, number ->
            val start = HexText.skipBlanks(text, lineStart, lineEnd)
            val end = HexText.trimEnd(text, start, lineEnd)
            if (start == end) return@forEachLine

            if (endSeen) {
                afterEnd++
                return@forEachLine
            }

            if (text[start].toInt() != ':'.code) {
                problem(false, number, start - lineStart + 1, "Not a record (it doesn't start with ':'), skipped")
                return@forEachLine
            }

            // The record's bytes after the colon
            val digits = end - start - 1
            if (digits < 10 || digits % 2 != 0) {
                problem(true, number, 1, "Record is too short or has an odd number of digits, skipped")
                return@forEachLine
            }
            val count = digits / 2
            if (count > record.size) {
                problem(true, number, 1, "Record is longer than any record can be, skipped")
                return@forEachLine
            }
            for (i in 0 until count) {
                val value = HexText.byteAt(text, start + 1 + i * 2)
                if (value < 0) {
                    problem(true, number, start - lineStart + 2 + i * 2, "Record has a character that isn't a hex digit, skipped")
                    return@forEachLine
                }
                record[i] = value.toByte()
            }

            val length = record[0].toInt() and 0xFF
            if (count != length + 5) {
                problem(true, number, 2, "Record says it holds $length bytes but has ${count - 5}, skipped")
                return@forEachLine
            }

            var sum = 0
            for (i in 0 until count) sum += record[i].toInt() and 0xFF
            if (sum and 0xFF != 0) {
                val expected = (-(sum - (record[count - 1].toInt() and 0xFF))) and 0xFF
                problem(
                    true, number, end - lineStart - 1,
                    "Record's checksum is wrong (${hexByte(record[count - 1].toInt() and 0xFF)}, should be ${hexByte(expected)}), skipped"
                )
                return@forEachLine
            }

            val offset = ((record[1].toInt() and 0xFF) shl 8) or (record[2].toInt() and 0xFF)
            val type = record[3].toInt() and 0xFF
            fun word(at: Int) = ((record[at].toLong() and 0xFF) shl 8) or (record[at + 1].toLong() and 0xFF)

            when (type) {
                DATA -> builder.put(base + offset, record, 4, length)
                END_OF_FILE -> endSeen = true
                EXTENDED_SEGMENT, EXTENDED_LINEAR -> {
                    if (length != 2) {
                        problem(true, number, 2, "Extended address record should hold 2 bytes, not $length, skipped")
                    } else {
                        base = if (type == EXTENDED_LINEAR) word(4) shl 16 else word(4) shl 4
                    }
                }
                START_SEGMENT, START_LINEAR -> {
                    if (length != 4) {
                        problem(true, number, 2, "Start address record should hold 4 bytes, not $length, skipped")
                    } else {
                        builder.startAddress = if (type == START_LINEAR) (word(4) shl 16) or word(6) else (word(4) shl 4) + word(6)
                    }
                }
                else -> problem(false, number, 8, "Unknown record type ${hexByte(type)}, skipped")
            }
        }

        if (unreported > 0) diagnostics.error("…and $unreported more problems like these")
        if (!endSeen) diagnostics.warning("The file has no end of file record, it may have been cut short")
        if (afterEnd > 0) diagnostics.warning("$afterEnd lines after the end of file record were ignored")
        if (builder.isEmpty) return TkResult.Failure(ImportError.NO_DATA)

        val built = builder.build()
        built.overlaps.forEach { diagnostics.warning("Data at $it is written more than once; the last record's bytes were kept") }
        return TkResult.Success(Imported(built.image, diagnostics.all))
    }

    /**
     * The image as Intel HEX, with extended linear address (04) records, a start linear address (05) record when it
     * has a start address, and an end of file record
     * @param bytesPerRecord Data bytes on each line, 1 to 255. 16 and 32 are common
     */
    fun write(image: MemoryImage, bytesPerRecord: Int = 16, lineEnding: String = "\r\n"): TkResult<String, ExportError> {
        require(bytesPerRecord in 1..255) { "Records hold 1 to 255 bytes" }
        val span = image.span ?: return TkResult.Failure(ExportError.EMPTY_IMAGE)
        if (span.endExclusive > 0x1_0000_0000L) return TkResult.Failure(ExportError.ADDRESS_TOO_HIGH)
        if ((image.startAddress ?: 0) > 0xFFFF_FFFFL) return TkResult.Failure(ExportError.ADDRESS_TOO_HIGH)

        val out = StringBuilder((image.size * 2.6 + 64).toInt())
        var upper = -1L
        val payload = ByteArray(255)

        fun record(type: Int, offset: Int, data: ByteArray, length: Int) {
            out.append(':')
            var sum = length + (offset shr 8) + (offset and 0xFF) + type
            HexText.appendByte(out, length)
            HexText.appendByte(out, offset shr 8)
            HexText.appendByte(out, offset and 0xFF)
            HexText.appendByte(out, type)
            for (i in 0 until length) {
                val value = data[i].toInt() and 0xFF
                sum += value
                HexText.appendByte(out, value)
            }
            HexText.appendByte(out, (-sum) and 0xFF)
            out.append(lineEnding)
        }

        image.ranges.forEachIndexed { index, range ->
            val bytes = image.segmentBytes(index)
            var position = 0
            while (position < bytes.size) {
                val address = range.start + position
                val high = address ushr 16
                if (high != upper) {
                    upper = high
                    payload[0] = (high shr 8).toByte()
                    payload[1] = high.toByte()
                    record(EXTENDED_LINEAR, 0, payload, 2)
                }
                // Records don't cross a 64K boundary, the next one needs a new 04 record
                val low = (address and 0xFFFF).toInt()
                val length = minOf(bytesPerRecord, bytes.size - position, 0x10000 - low)
                bytes.copyInto(payload, 0, position, position + length)
                record(DATA, low, payload, length)
                position += length
            }
        }

        image.startAddress?.let { start ->
            for (i in 0 until 4) payload[i] = (start shr (24 - i * 8)).toByte()
            record(START_LINEAR, 0, payload, 4)
        }
        record(END_OF_FILE, 0, payload, 0)
        return TkResult.Success(out.toString())
    }

    private fun hexByte(value: Int) = "0x" + value.toString(16).uppercase().padStart(2, '0')
}
