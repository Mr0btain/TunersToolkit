package components.parsers.hex.intel

fun calculateIntelHexChecksum(line: String): Int {
    val len = line.length-3
    var checksum = 0
    for (i in 1..len step 2) {
        val num = line.substring(i, i+2).toInt(radix = 16)
        checksum += num
    }
    checksum = ((0xFF-(checksum and 0xFF))+1) and 0xFF
    return checksum
}