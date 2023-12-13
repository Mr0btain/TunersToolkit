package components.parsers.hex

import components.parsers.a2l.classes.enums.DataType

/**
 * A Segment of data and its Start, End, and Base offset
 */
data class ByteSegmentData(
    val baseOffset: ULong = ULong.MIN_VALUE,
    val startOffset: ULong = ULong.MIN_VALUE,
    val data: ByteArray = byteArrayOf(),
    val endOffset: ULong = ULong.MIN_VALUE
)

/**
 * Finds the Byte Segment that contains the Address
 */

fun getSegmentId(
    segments: Map<String, HexSegmentData>,
    dataType: DataType,
    position: ULong
): String {
    // Get length so we can make sure we don't exceed the length of the segment
    val mLength = dataType.size

    // Find the containing segment
    segments.forEach {
        if (it.value.baseOffset < position && it.value.endOffset > position) {
            if (it.value.endOffset >= mLength.toULong()) {
                return it.key
            }
        }
    }

    // If not found return empty string
    return ""
}