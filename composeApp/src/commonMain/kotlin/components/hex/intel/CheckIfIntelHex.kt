package components.hex.intel

/**
 * Returns true if data is from an Intel hex file
 */
fun checkIfIntelHex(testLine: String): Boolean {
    val checksum = testLine.readIntelHexChecksum()

    return if (testLine.length < 400) {
        when {
            (checksum == calculateIntelHexChecksum(testLine) && testLine.startsWith(":")) -> {
                true
            }

            else -> { false }
        }
    } else { false }
}