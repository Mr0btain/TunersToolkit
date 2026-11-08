package org.mr0btain.tunerstoolkit.core.checksum

import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/**
 * A way of turning bytes into a checksum. Algorithms only see bytes, in order; which bytes and where the result goes
 * is the [ChecksumCheck]'s business. Results are [width] bits, in the low bits of a ULong
 */
sealed interface ChecksumAlgorithm {
    val width: Int
    val label: String

    /** A fresh running state, fed range by range so ranges don't have to be copied into one array */
    fun start(): Running

    interface Running {
        fun update(bytes: ByteArray)
        fun finish(): ULong
    }

    fun compute(vararg chunks: ByteArray): ULong = start().apply { chunks.forEach(::update) }.finish()

    /**
     * Adds up [word]-byte words (read in [byteOrder]) into a [width]-bit total that wraps round, starting from [init].
     * A last partial word counts as zero-padded at its end
     */
    data class Sum(val word: Int = 1, override val width: Int = 16, val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, val init: ULong = 0u) : ChecksumAlgorithm {
        init {
            require(word in listOf(1, 2, 4, 8)) { "Words are 1, 2, 4 or 8 bytes" }
            require(width in listOf(8, 16, 32, 64)) { "Sums are 8, 16, 32 or 64 bits" }
        }

        override val label: String get() = "${width}-bit sum of ${if (word == 1) "bytes" else "${word * 8}-bit words (${order(byteOrder)})"}"

        override fun start(): Running = WordRunning(word, byteOrder, width) { acc, value -> acc + value }.also { it.acc = init }
    }

    /** XORs [word]-byte words together */
    data class Xor(val word: Int = 1, val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN, val init: ULong = 0u) : ChecksumAlgorithm {
        init {
            require(word in listOf(1, 2, 4, 8)) { "Words are 1, 2, 4 or 8 bytes" }
        }

        override val width: Int get() = word * 8
        override val label: String get() = "XOR of ${if (word == 1) "bytes" else "${word * 8}-bit words (${order(byteOrder)})"}"
        override fun start(): Running = WordRunning(word, byteOrder, width) { acc, value -> acc xor value }.also { it.acc = init }
    }

    /**
     * A CRC in the usual parameter model (width, poly, init, refIn, refOut, xorOut; as catalogued by Greg Cook's CRC
     * catalogue). Up to 32 bits, table driven
     */
    data class Crc(
        override val width: Int,
        val poly: ULong,
        val init: ULong,
        val refIn: Boolean,
        val refOut: Boolean,
        val xorOut: ULong,
        val name: String? = null
    ) : ChecksumAlgorithm {
        init {
            require(width in 1..32) { "CRCs from 1 to 32 bits are supported, not $width" }
        }

        override val label: String get() = name ?: "CRC-$width poly 0x${poly.toString(16).uppercase()}"
        private val mask: ULong = (1uL shl width) - 1u
        private val table: Array<ULong> by lazy { buildTable() }

        /** Bit by bit, MSB first with the register in the top of 32 bits; reflected inputs are bit-reversed first */
        private fun buildTable(): Array<ULong> = Array(256) { n ->
            var register = (n.toULong() shl 24)
            repeat(8) {
                register = if (register and 0x80000000uL != 0uL) (register shl 1) xor (poly shl (32 - width)) else register shl 1
                register = register and 0xFFFFFFFFuL
            }
            register
        }

        override fun start(): Running = object : Running {
            var register = (init and mask) shl (32 - width)

            override fun update(bytes: ByteArray) {
                for (b in bytes) {
                    var value = b.toInt() and 0xFF
                    if (refIn) value = reverse8(value)
                    val index = ((register shr 24) xor value.toULong()) and 0xFFu
                    register = ((register shl 8) xor table[index.toInt()]) and 0xFFFFFFFFuL
                }
            }

            override fun finish(): ULong {
                var result = (register shr (32 - width)) and mask
                if (refOut) result = reverse(result, width)
                return (result xor xorOut) and mask
            }
        }
    }

    /** Adler-32, as zlib computes it */
    data object Adler32 : ChecksumAlgorithm {
        override val width: Int get() = 32
        override val label: String get() = "Adler-32"

        override fun start(): Running = object : Running {
            var a = 1L
            var b = 0L
            override fun update(bytes: ByteArray) {
                for (byte in bytes) {
                    a = (a + (byte.toInt() and 0xFF)) % 65521
                    b = (b + a) % 65521
                }
            }

            override fun finish(): ULong = ((b shl 16) or a).toULong()
        }
    }

    companion object {
        /** Common CRCs, by their catalogue names. The check value of "123456789" is in the tests */
        val CRCS: List<Crc> = listOf(
            Crc(8, 0x07u, 0x00u, false, false, 0x00u, "CRC-8/SMBUS"),
            Crc(8, 0x1Du, 0xFFu, false, false, 0xFFu, "CRC-8/SAE-J1850"),
            Crc(16, 0x8005u, 0x0000u, true, true, 0x0000u, "CRC-16/ARC"),
            Crc(16, 0x8005u, 0xFFFFu, true, true, 0x0000u, "CRC-16/MODBUS"),
            Crc(16, 0x1021u, 0xFFFFu, false, false, 0x0000u, "CRC-16/IBM-3740"),
            Crc(16, 0x1021u, 0x0000u, false, false, 0x0000u, "CRC-16/XMODEM"),
            Crc(16, 0x1021u, 0x0000u, true, true, 0x0000u, "CRC-16/KERMIT"),
            Crc(32, 0x04C11DB7u, 0xFFFFFFFFu, true, true, 0xFFFFFFFFu, "CRC-32/ISO-HDLC"),
            Crc(32, 0x04C11DB7u, 0xFFFFFFFFu, false, false, 0xFFFFFFFFu, "CRC-32/BZIP2"),
            Crc(32, 0x04C11DB7u, 0xFFFFFFFFu, false, false, 0x00000000u, "CRC-32/MPEG-2"),
            Crc(32, 0x1EDC6F41u, 0xFFFFFFFFu, true, true, 0xFFFFFFFFu, "CRC-32/ISCSI")
        )

        fun crc(name: String): Crc? = CRCS.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: if (name.equals("CRC-32", ignoreCase = true)) CRCS.first { it.name == "CRC-32/ISO-HDLC" } else null

        private fun order(order: ByteOrder) = if (order == ByteOrder.LITTLE_ENDIAN) "LSB first" else "MSB first"

        internal fun reverse8(value: Int): Int {
            var v = value
            var r = 0
            repeat(8) {
                r = (r shl 1) or (v and 1)
                v = v shr 1
            }
            return r
        }

        internal fun reverse(value: ULong, bits: Int): ULong {
            var v = value
            var r = 0uL
            repeat(bits) {
                r = (r shl 1) or (v and 1u)
                v = v shr 1
            }
            return r
        }
    }
}

/** Reads words across chunk boundaries and folds them with [combine], the total wrapping at the algorithm's width */
private class WordRunning(val word: Int, val order: ByteOrder, width: Int, val combine: (ULong, ULong) -> ULong) : ChecksumAlgorithm.Running {
    private val mask: ULong = if (width >= 64) ULong.MAX_VALUE else (1uL shl width) - 1u
    var acc: ULong = 0u
    private val pending = ByteArray(word)
    private var filled = 0

    override fun update(bytes: ByteArray) {
        for (b in bytes) {
            pending[filled++] = b
            if (filled == word) {
                acc = combine(acc, order.decode(pending, 0, word))
                filled = 0
            }
        }
    }

    override fun finish(): ULong {
        if (filled > 0) {
            for (i in filled until word) pending[i] = 0
            acc = combine(acc, order.decode(pending, 0, word))
            filled = 0
        }
        return acc and mask
    }
}
