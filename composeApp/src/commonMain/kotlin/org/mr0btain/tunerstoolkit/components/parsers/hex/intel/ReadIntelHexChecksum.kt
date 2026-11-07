package org.mr0btain.tunerstoolkit.components.parsers.hex.intel
fun String.readIntelHexChecksum(): Int {
    return this.substring(this.length-2, this.length).toInt(radix = 16)
}