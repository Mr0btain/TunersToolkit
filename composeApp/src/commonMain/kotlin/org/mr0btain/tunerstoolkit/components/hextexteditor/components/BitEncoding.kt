package org.mr0btain.tunerstoolkit.components.hextexteditor.components
enum class BitEncoding {
    EIGHT,
    SIXTEEN,
    THIRTYTWO
}

fun BitEncoding.number(): Int {
    return when (this.name) {
        "EIGHT" -> 2
        "SIXTEEN" -> 4
        "THIRTYTWO" -> 6
        else -> 0
    }
}