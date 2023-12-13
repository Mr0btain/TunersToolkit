package components.calibrationdata.helpers

/**
 * Apply a BitMask to a Number and return the result as a Double
 */
fun Number.applyBitMask(bitMask: ULong): Double {
    val result = this.toInt() and bitMask.toInt()
    var shift = 0
    val bits = result.toString(2)

    for (i in 0..bits.length) {
        val j = bits.length - 1 - i
        if (bits[j] == '1') {
            shift = i
            break
        }
    }

    return (result shr(shift)).toDouble()
}