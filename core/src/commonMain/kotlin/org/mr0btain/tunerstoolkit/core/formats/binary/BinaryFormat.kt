package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/** The ways an ECU image can be stored */
enum class BinaryFormat(val label: String, val extensions: List<String>) {
    INTEL_HEX("Intel HEX", listOf("hex", "ihex", "ihx")),
    S_RECORD("Motorola S-record", listOf("s19", "s28", "s37", "srec", "mot", "s")),
    RAW("Raw binary", listOf("bin", "ori", "rom"));

    companion object {
        /**
         * The format [bytes] are in, from what's in them, not the file name. A file starting with an Intel HEX or
         * S-record is that format; anything else is raw binary
         */
        fun detect(bytes: ByteArray): BinaryFormat {
            val first = HexText.firstContentIndex(bytes)
            if (first < 0) return RAW
            val c = bytes[first].toInt()
            return when {
                c == ':'.code && hexRun(bytes, first + 1) >= 10 -> INTEL_HEX
                (c == 'S'.code || c == 's'.code) && first + 1 < bytes.size &&
                    bytes[first + 1].toInt() in '0'.code..'9'.code && hexRun(bytes, first + 2) >= 4 -> S_RECORD
                else -> RAW
            }
        }

        /** How many hex digits follow [start] */
        private fun hexRun(bytes: ByteArray, start: Int): Int {
            var i = start
            while (i < bytes.size && HexText.digit(bytes[i].toInt()) >= 0) i++
            return i - start
        }
    }
}

/** Reads an ECU image in whichever format it's in */
object BinaryImport {
    /**
     * @param rawBaseAddress Where a raw binary starts in memory; HEX and S-record files say so themselves
     * @param format Read as this format instead of detecting it
     */
    fun read(
        bytes: ByteArray,
        fileName: String,
        rawBaseAddress: Long = 0,
        format: BinaryFormat = BinaryFormat.detect(bytes)
    ): TkResult<Imported<MemoryImage>, ImportError> = when (format) {
        BinaryFormat.INTEL_HEX -> IntelHex.read(bytes, fileName)
        BinaryFormat.S_RECORD -> SRecord.read(bytes, fileName)
        BinaryFormat.RAW -> RawBinary.read(bytes, rawBaseAddress)
    }
}
