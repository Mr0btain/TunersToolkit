package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/**
 * Whether an image is the one an A2L describes: MOD_PAR's EPK is a text that the software also holds at ADDR_EPK.
 * When they differ, the A2L is for other software and its addresses can't be trusted
 */
sealed interface EpkResult {
    /** The A2L has no EPK or no ADDR_EPK, so there's nothing to check */
    data object NotDescribed : EpkResult

    data class Match(val address: Long, val epk: String) : EpkResult

    /** [found] is what's at [address], read as text the length of [expected] */
    data class Mismatch(val address: Long, val expected: String, val found: String) : EpkResult

    /** None of the ADDR_EPK addresses are in the image */
    data class NotInImage(val addresses: List<Long>, val expected: String) : EpkResult
}

object EpkCheck {
    /**
     * Checks each ADDR_EPK in turn: a match at any of them is a match. ECU_CALIBRATION_OFFSET isn't added, since
     * ADDR_EPK is where the software keeps it
     */
    fun check(module: A2lModule, image: MemoryImage): EpkResult {
        val modPar = module.modPar ?: return EpkResult.NotDescribed
        val expected = modPar.epk?.takeIf { it.isNotEmpty() } ?: return EpkResult.NotDescribed
        if (modPar.addrEpk.isEmpty()) return EpkResult.NotDescribed
        val length = expected.encodeToByteArray().size
        var mismatch: EpkResult.Mismatch? = null
        for (address in modPar.addrEpk) {
            val bytes = image.readOrNull(address, length) ?: continue
            val found = bytes.toLatin1()
            if (found == expected || bytes.decodeToString() == expected) return EpkResult.Match(address, expected)
            if (mismatch == null) mismatch = EpkResult.Mismatch(address, expected, found.printable())
        }
        return mismatch ?: EpkResult.NotInImage(modPar.addrEpk, expected)
    }

    private fun ByteArray.toLatin1(): String = buildString(size) { this@toLatin1.forEach { append((it.toInt() and 0xFF).toChar()) } }

    /** Bytes that aren't printable shown as dots, so a mismatch can be shown */
    private fun String.printable(): String = map { if (it.code in 0x20..0x7E) it else '.' }.joinToString("")
}
