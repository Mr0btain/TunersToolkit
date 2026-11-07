package org.mr0btain.tunerstoolkit.components.testing
import kotlin.random.Random

fun randomHexString(numDigits: Int): String {
    val hexChars = "0123456789ABCDEF"
    val random = Random
    val sb = StringBuilder()

    repeat(numDigits) {
        val hexDigit = hexChars[random.nextInt(16)]
        sb.append(hexDigit)
    }

    return sb.toString()
}