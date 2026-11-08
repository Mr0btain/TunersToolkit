package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostics
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/**
 * Motorola S-record (S19, S28, S37): `S<type><count><address><data><checksum>` lines. Types:
 * - S0 header, kept as [MemoryImage.header]
 * - S1, S2, S3 data with a 16, 24 or 32 bit address
 * - S5, S6 how many data records came before (16 or 24 bit), checked against what was read
 * - S7, S8, S9 start address (32, 24 or 16 bit), kept as [MemoryImage.startAddress]
 *
 * The count byte covers the address, data and checksum. The checksum is the ones' complement of the low byte of the
 * sum of the count, address and data bytes. Reading never throws: a broken record is left out and reported.
 */
object SRecord {
    /** Address bytes for each record type, 0 for S4 (reserved) */
    private val ADDRESS_BYTES = intArrayOf(2, 2, 3, 4, 0, 2, 3, 4, 3, 2)

    private const val MAX_REPORTED = 200

    /** Which data records [write] uses */
    enum class AddressSize(val dataType: Int, val endType: Int, val bytes: Int) {
        /** S1 data and S9 start, addresses up to 0xFFFF */
        S19(1, 9, 2),

        /** S2 data and S8 start, addresses up to 0xFFFFFF */
        S28(2, 8, 3),

        /** S3 data and S7 start, addresses up to 0xFFFFFFFF */
        S37(3, 7, 4);

        val limit: Long get() = 1L shl (bytes * 8)
    }

    fun read(text: ByteArray, fileName: String = "file.s19"): TkResult<Imported<MemoryImage>, ImportError> {
        if (HexText.firstContentIndex(text) < 0) return TkResult.Failure(ImportError.EMPTY_FILE)

        val diagnostics = Diagnostics()
        val builder = MemoryImage.Builder()
        var dataRecords = 0L
        var countSeen: Long? = null
        var endSeen = false
        var reported = 0
        var unreported = 0
        val record = ByteArray(256)

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

            val first = text[start].toInt()
            if (first != 'S'.code && first != 's'.code) {
                problem(false, number, start - lineStart + 1, "Not a record (it doesn't start with 'S'), skipped")
                return@forEachLine
            }
            if (end - start < 2) {
                problem(true, number, 1, "Record is too short, skipped")
                return@forEachLine
            }
            val type = text[start + 1].toInt() - '0'.code
            if (type !in 0..9) {
                problem(true, number, 2, "Unknown record type, skipped")
                return@forEachLine
            }
            if (type == 4) {
                problem(false, number, 2, "S4 records are reserved, skipped")
                return@forEachLine
            }

            val digits = end - start - 2
            if (digits < 2 || digits % 2 != 0) {
                problem(true, number, 3, "Record is too short or has an odd number of digits, skipped")
                return@forEachLine
            }
            val count = digits / 2
            if (count > record.size) {
                problem(true, number, 3, "Record is longer than any record can be, skipped")
                return@forEachLine
            }
            for (i in 0 until count) {
                val value = HexText.byteAt(text, start + 2 + i * 2)
                if (value < 0) {
                    problem(true, number, start - lineStart + 3 + i * 2, "Record has a character that isn't a hex digit, skipped")
                    return@forEachLine
                }
                record[i] = value.toByte()
            }

            val length = record[0].toInt() and 0xFF
            if (length != count - 1) {
                problem(true, number, 3, "Record says it holds $length bytes but has ${count - 1}, skipped")
                return@forEachLine
            }
            val addressBytes = ADDRESS_BYTES[type]
            if (length < addressBytes + 1) {
                problem(true, number, 3, "Record is too short for its S$type address, skipped")
                return@forEachLine
            }

            var sum = 0
            for (i in 0 until count - 1) sum += record[i].toInt() and 0xFF
            val expected = sum.inv() and 0xFF
            val checksum = record[count - 1].toInt() and 0xFF
            if (checksum != expected) {
                problem(true, number, end - lineStart - 1, "Record's checksum is wrong (${hexByte(checksum)}, should be ${hexByte(expected)}), skipped")
                return@forEachLine
            }

            var address = 0L
            for (i in 1..addressBytes) address = (address shl 8) or (record[i].toLong() and 0xFF)
            val dataStart = 1 + addressBytes
            val dataLength = count - 1 - dataStart

            when (type) {
                0 -> builder.header = headerText(record, dataStart, dataLength)
                1, 2, 3 -> {
                    builder.put(address, record, dataStart, dataLength)
                    dataRecords++
                }
                5, 6 -> countSeen = address
                7, 8, 9 -> {
                    builder.startAddress = address
                    endSeen = true
                }
            }
        }

        if (unreported > 0) diagnostics.error("…and $unreported more problems like these")
        countSeen?.let { if (it != dataRecords) diagnostics.warning("The file's count record says $it data records, but $dataRecords were read") }
        if (!endSeen) diagnostics.warning("The file has no S7, S8 or S9 end record, it may have been cut short")
        if (builder.isEmpty) return TkResult.Failure(ImportError.NO_DATA)

        val built = builder.build()
        built.overlaps.forEach { diagnostics.warning("Data at $it is written more than once; the last record's bytes were kept") }
        return TkResult.Success(Imported(built.image, diagnostics.all))
    }

    /**
     * The image as S-records: an S0 header ([MemoryImage.header], or [defaultHeader]), data records, an S5 or S6
     * count when it fits, and an S7, S8 or S9 end record with the start address (0 without one)
     * @param addressSize The smallest that fits the image when null
     * @param bytesPerRecord Data bytes on each line; 16 and 32 are common
     */
    fun write(
        image: MemoryImage,
        addressSize: AddressSize? = null,
        bytesPerRecord: Int = 32,
        defaultHeader: String = "",
        lineEnding: String = "\r\n"
    ): TkResult<String, ExportError> {
        val span = image.span ?: return TkResult.Failure(ExportError.EMPTY_IMAGE)
        val highest = maxOf(span.last, image.startAddress ?: 0)
        val size = addressSize ?: AddressSize.entries.first { highest < it.limit }
        if (highest >= size.limit) return TkResult.Failure(ExportError.ADDRESS_TOO_HIGH)
        require(bytesPerRecord in 1..(255 - size.bytes - 1)) { "S${size.dataType} records hold 1 to ${255 - size.bytes - 1} bytes" }

        val out = StringBuilder((image.size * 2.6 + 64).toInt())
        val payload = ByteArray(255)

        fun record(type: Int, address: Long, addressBytes: Int, data: ByteArray, length: Int) {
            val count = addressBytes + length + 1
            out.append('S').append(('0'.code + type).toChar())
            var sum = count
            HexText.appendByte(out, count)
            for (i in addressBytes - 1 downTo 0) {
                val value = ((address shr (i * 8)) and 0xFF).toInt()
                sum += value
                HexText.appendByte(out, value)
            }
            for (i in 0 until length) {
                val value = data[i].toInt() and 0xFF
                sum += value
                HexText.appendByte(out, value)
            }
            HexText.appendByte(out, sum.inv() and 0xFF)
            out.append(lineEnding)
        }

        val header = (image.header ?: defaultHeader).encodeToByteArray().take(250).toByteArray()
        record(0, 0, 2, header, header.size)

        var dataRecords = 0L
        image.ranges.forEachIndexed { index, range ->
            val bytes = image.segmentBytes(index)
            var position = 0
            while (position < bytes.size) {
                val length = minOf(bytesPerRecord, bytes.size - position)
                bytes.copyInto(payload, 0, position, position + length)
                record(size.dataType, range.start + position, size.bytes, payload, length)
                position += length
                dataRecords++
            }
        }

        when {
            dataRecords <= 0xFFFF -> record(5, dataRecords, 2, payload, 0)
            dataRecords <= 0xFFFFFF -> record(6, dataRecords, 3, payload, 0)
        }
        record(size.endType, image.startAddress ?: 0, size.bytes, payload, 0)
        return TkResult.Success(out.toString())
    }

    /** S0 data as text, printable ASCII only */
    private fun headerText(record: ByteArray, start: Int, length: Int): String = buildString {
        for (i in start until start + length) {
            val c = record[i].toInt() and 0xFF
            if (c in 0x20..0x7E) append(c.toChar())
        }
    }.trim()

    private fun hexByte(value: Int) = "0x" + value.toString(16).uppercase().padStart(2, '0')
}
