package components.parsers.a2l.modcommon

import components.parsers.a2l.classes.enums.ByteOrder
import components.parsers.a2l.classes.enums.DataSize
import components.parsers.a2l.classes.enums.Deposit
import kotlinx.serialization.Serializable

/**
 * This keyword is used to specify general description data for the module, which are then
 * used as standard in this module. Should other methods be used for an object (e.g.
 * adjustable object or measurement object) of this module, this must then be indicated in
 * the description of the relevant object.
 * @param alignmentByte Declares the alignment of bytes in the complete
 * module. The alignment is 1 if parameter is missing.
 * @param alignmentLong Declares the alignment of longs in the complete
 * module. The alignment is 4 if parameter is missing.
 * @param alignmentFloat16Ieee Declares the alignment of 16-bit floats in the
 * complete module. The alignment is 4 if parameter is
 * missing.
 * @param alignmentFloat32Ieee Declares the alignment of 32-bit floats in the
 * complete module. The alignment is 4 if parameter is
 * missing.
 * @param alignmentFloat64Ieee Declares the alignment of 64-bit floats in the
 * complete module. The alignment is 4 if parameter is
 * missing.
 * @param alignmentInt64 Declares the alignment of int64 in the complete
 * module. The alignment is 8 if parameter is missing.
 * @param alignmentWord Declares the alignment of words in the complete
 * module. The alignment is 2 if parameter is missing.
 * @param byteOrder Byte order for the whole device. If this optional
 * parameter is not declared, MSB_LAST (Intel format)
 * is used as a default.
 * @param dataSize Data size in bits
 * @param deposit Standard deposit mode for axis points: ABSOLUTE or DIFFERENCE
 */

@Serializable
data class ModCommon(
    val alignmentByte: Int = 1,
    val alignmentLong: Int = 4,
    val alignmentFloat16Ieee: Int = 4,
    val alignmentFloat32Ieee: Int = 4,
    val alignmentFloat64Ieee: Int = 4,
    val alignmentInt64: Int = 8,
    val alignmentWord: Int = 2,
    val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    val dataSize: DataSize = DataSize.NOT_SET,
    val deposit: Deposit = Deposit.NOT_SET
)