package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/**
 * A plain dump of memory with no addresses in it, like most ECU reads (.bin, .ori). Where it sits in memory comes
 * from outside the file: the user, or the A2L's MEMORY_SEGMENT
 */
object RawBinary {
    /** The whole file as one segment starting at [baseAddress] */
    fun read(bytes: ByteArray, baseAddress: Long = 0): TkResult<Imported<MemoryImage>, ImportError> {
        require(baseAddress >= 0) { "The base address can't be negative" }
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        return TkResult.Success(Imported(MemoryImage.of(baseAddress to bytes)))
    }

    /**
     * The bytes of [range] (the whole image when null), gaps filled with [fill]
     * @param fill 0xFF by default, what erased flash reads as
     */
    fun write(image: MemoryImage, range: AddressRange? = null, fill: Byte = 0xFF.toByte()): TkResult<ByteArray, ExportError> {
        val span = range ?: image.span ?: return TkResult.Failure(ExportError.EMPTY_IMAGE)
        if (span.size > Int.MAX_VALUE - 8) return TkResult.Failure(ExportError.TOO_BIG)

        val out = ByteArray(span.size.toInt())
        val present = BooleanArray(out.size)
        image.readInto(span.start, out.size, out, present)
        for (i in out.indices) if (!present[i]) out[i] = fill
        return TkResult.Success(out)
    }
}
