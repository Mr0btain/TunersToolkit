package org.mr0btain.tunerstoolkit.components.calibrationdata.classes
/**
 * The Resulting Data from the HexSegments
 * @param startPosition The absolute start position as it is in the HexSegment
 * @param hexData The Raw Hex data from the address with Bitmask applied
 * @param endPosition The absolute end position as it is in the HexSegment
 * @param segmentId The String key used to access the correct segment data
 * @param formattedValues List of values formatted to a Number
 * @param baseOffset The base offset
 */
data class HexDataResult(
    val startPosition: Int = Int.MIN_VALUE,
    val hexData: List<String> = listOf(),
    val endPosition: Int = Int.MIN_VALUE,
    val segmentId: String = "",
    val formattedValues: List<Number> = listOf(),
    val baseOffset: ULong = ULong.MIN_VALUE
)

/**
 * Result from pulling data from the Hex Segments of data
 * @param startPosition The absolute start position as it is in the HexSegment
 * @param hexData The Raw Hex data from the address, with bitmask applied
 * @param segChunkMapKey The Key from the entry in the Chunk Map for access later
 */
data class RawDataResult(
    val startPosition: Int = Int.MIN_VALUE,
    val hexData: String = ""
)
