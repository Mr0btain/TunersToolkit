package components.parsers.formula.components

/**
 * Returns the Factorial of whatever number as Double
 */
fun Number.factorial(): Double {
    var sum = 1.0
    var count = 2.0
    while (this.toDouble() <= count) {
        sum *= count
        count++
    }
    return sum
}

/**
 * The Fast inverse method for calculating the square root of a number, really fast but not super accurate
 */
fun Number.fastInvSqrt(): Double {
    var i: Int = this.toFloat().toRawBits() // Step 1: Bit-level representation of the floating-point number

    i = 0x5f3759df - (i ushr 1) // Step 2: Magic number and bit manipulation
    var y: Float = i.toFloat() // Step 3: Convert the bits back to float

    // Step 4: Iterative refinement
    y *= (1.5f - 0.5f * this.toFloat() * y * y)
    y *= (1.5f - 0.5f * this.toFloat() * y * y)

    return y.toDouble()
}