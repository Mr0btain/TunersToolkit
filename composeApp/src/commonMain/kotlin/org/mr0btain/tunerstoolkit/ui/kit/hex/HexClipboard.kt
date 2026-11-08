package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.platform.Clipboard
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteText
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/** Puts plain text on the system clipboard */
expect suspend fun Clipboard.writeText(text: String)

/** The system clipboard's plain text, or null when it holds none (or the platform won't say) */
expect suspend fun Clipboard.readText(): String?

/** Copying and pasting bytes as text, kept apart from the view so it can be tested */
object HexClipboard {
    /** Most bytes copied at once: a whole 4 MB flash as hex is already 12 MB of text */
    const val MAX_COPY = 4 * 1024 * 1024

    /**
     * The bytes in [range] as text: "1A 2B 3C", or as the ASCII column shows them when [ascii]. Bytes in gaps between
     * segments are left out. Null when there's nothing there, or more than [MAX_COPY]
     */
    fun copy(image: MemoryImage, range: AddressRange, ascii: Boolean): String? {
        if (range.isEmpty || range.size > MAX_COPY) return null
        val size = range.size.toInt()
        val bytes = ByteArray(size)
        val present = BooleanArray(size)
        image.readInto(range.start, size, bytes, present)
        val kept = bytes.filterIndexed { index, _ -> present[index] }.toByteArray()
        if (kept.isEmpty()) return null
        return if (ascii) ByteText.toAscii(kept) else ByteText.toHex(kept)
    }

    /** What pasting would do */
    sealed interface Paste {
        /** Write [bytes] at [address]; [dropped] more didn't fit before the segment ends */
        data class Write(val address: Long, val bytes: ByteArray, val dropped: Int) : Paste
        data object NotHex : Paste
        data object Empty : Paste
        data object Outside : Paste
    }

    /**
     * Pasting [text] over the bytes from [at]: as hex in the hex column, as characters in the [ascii] one. Pasting
     * overwrites (an image's layout never changes), and stops at the end of the segment
     */
    fun paste(image: MemoryImage, at: Long, text: String, ascii: Boolean): Paste {
        if (text.isEmpty()) return Paste.Empty
        val bytes = if (ascii) ByteText.fromText(text) else ByteText.parseHex(text) ?: return Paste.NotHex
        val segment = image.rangeAt(at) ?: return Paste.Outside
        val room = (segment.endExclusive - at).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return if (bytes.size <= room) Paste.Write(at, bytes, 0) else Paste.Write(at, bytes.copyOf(room), bytes.size - room)
    }
}
