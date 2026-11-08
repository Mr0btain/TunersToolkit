package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/** How cell values are written */
enum class HexRadix(val label: String) {
    HEX("Hex"),
    UNSIGNED("Unsigned"),
    SIGNED("Signed")
}

/**
 * What each cell of the hex view shows: [wordSize] bytes (1, 2, 4 or 8) read in [order], written in [radix]
 */
data class HexDisplay(
    val wordSize: Int = 1,
    val order: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val radix: HexRadix = HexRadix.HEX
) {
    init {
        require(wordSize in setOf(1, 2, 4, 8)) { "Cells are 1, 2, 4 or 8 bytes" }
    }

    /** Characters each cell takes, the widest value right aligned */
    val cellChars: Int = when (radix) {
        HexRadix.HEX -> wordSize * 2
        HexRadix.UNSIGNED -> when (wordSize) { 1 -> 3; 2 -> 5; 4 -> 10; else -> 20 }
        HexRadix.SIGNED -> when (wordSize) { 1 -> 4; 2 -> 6; 4 -> 11; else -> 20 }
    }

    /** Typing digits into cells only works on plain hex bytes */
    val editable: Boolean get() = wordSize == 1 && radix == HexRadix.HEX

    /** The cell for [wordSize] bytes of [bytes] from [offset] */
    fun format(bytes: ByteArray, offset: Int): String {
        val unsigned = order.decode(bytes, offset, wordSize)
        val text = when (radix) {
            HexRadix.HEX -> unsigned.toString(16).uppercase().padStart(cellChars, '0')
            HexRadix.UNSIGNED -> unsigned.toString()
            HexRadix.SIGNED -> {
                val shift = 64 - wordSize * 8
                ((unsigned.toLong() shl shift) shr shift).toString()
            }
        }
        return text.padStart(cellChars)
    }
}

/**
 * Where the hex view is and what's selected. Positions are byte addresses, never text offsets, so they stay put
 * when the display changes
 */
@Stable
class HexViewState(bytesPerRow: Int = 16) {
    val listState = LazyListState()

    var bytesPerRow by mutableIntStateOf(bytesPerRow)
    var display by mutableStateOf(HexDisplay())

    /** The byte the cursor is on, null before anything is picked */
    var cursor by mutableStateOf<Long?>(null)
        private set

    /** Where the selection started; the selection runs from here to [cursor] */
    var anchor by mutableStateOf<Long?>(null)
        private set

    /** 0 when typing goes into the high digit of the cursor's byte, 1 for the low one */
    var nibble by mutableIntStateOf(0)
        internal set

    /** Typing goes into the ASCII column instead of the hex one */
    var asciiFocus by mutableStateOf(false)

    /** Briefly marked, like a search match */
    var highlight by mutableStateOf<AddressRange?>(null)

    var readOnly by mutableStateOf(false)

    /** An address the view should scroll to, with a count so asking for the same one twice still scrolls */
    internal var revealRequest by mutableStateOf<Pair<Long, Int>?>(null)
        private set

    /** The bytes between [anchor] and [cursor], both included. Null with only a cursor */
    val selection: AddressRange?
        get() {
            val c = cursor ?: return null
            val a = anchor ?: return null
            if (a == c) return null
            return AddressRange(minOf(a, c), maxOf(a, c) + 1)
        }

    /** Moves the cursor to [address]; [extend] keeps the anchor so the selection grows */
    fun moveTo(address: Long, extend: Boolean = false) {
        if (!extend || anchor == null) anchor = address
        cursor = address
        nibble = 0
    }

    fun select(range: AddressRange) {
        if (range.isEmpty) return
        anchor = range.start
        cursor = range.last
        nibble = 0
    }

    fun clearSelection() {
        anchor = cursor
    }

    /** Moves the cursor to [address] and scrolls the view to it */
    fun reveal(address: Long, extend: Boolean = false) {
        moveTo(address, extend)
        revealRequest = address to ((revealRequest?.second ?: 0) + 1)
    }
}

@Composable
fun rememberHexViewState(bytesPerRow: Int = 16): HexViewState = remember { HexViewState(bytesPerRow) }
