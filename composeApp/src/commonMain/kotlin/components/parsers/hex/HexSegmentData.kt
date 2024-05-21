package components.parsers.hex

import kotlinx.serialization.Serializable

/**
 * Wrapper for HexSegmentData for writing to disk
 */

@Serializable
data class HexSegmentMap(val data: Map<String, HexSegmentData>)

/**
 * A Segment of Hex data and its Start, End, and Base offset
 */

@Serializable
data class HexSegmentData(
    val data: Map<Int, String> = mapOf(),
    val baseOffset: ULong = ULong.MIN_VALUE,
    val startOffset: ULong = ULong.MIN_VALUE,
    val endOffset: ULong = ULong.MIN_VALUE,
    val totalLength: Int = Int.MIN_VALUE
)

/**
 * Chunks a String into a Map of Int(Starting Address) and String(Hex Data)
 */

fun String.chunkHexData(): Map<Int, String> {
    var position = 0
    val result = mutableMapOf<Int, String>()

    this.chunked(6000).forEach {
        result[position] = it
        position += it.length
    }

    return result
}

/**
 * Processes a ByteArray into a Map of Int(Position) and String(Hex Data)
 */

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.toHexMap(): Map<Int, String> {
    var position = 0
    val hex = this.joinToString("") { it.toHexString(HexFormat.UpperCase) }.chunked(6000)
    val result = mutableMapOf<Int, String>()

    hex.forEach { chunk ->
        result[position] = chunk
        position += chunk.length
    }

    return result
}