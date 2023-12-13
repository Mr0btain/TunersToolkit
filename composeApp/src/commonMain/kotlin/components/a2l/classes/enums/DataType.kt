package components.a2l.classes.enums

import kotlinx.serialization.Serializable

/**
 * Numeration for description of the basic data types in the ECU program
 */
@Serializable
enum class DataType(val size: Int) {
    UBYTE(1),
    SBYTE(1),
    UWORD(2),
    SWORD(2),
    ULONG(4),
    SLONG(4),
    AUint64(8),
    AInt64(8),
    Float16Ieee(2), // Assuming you represent Float16 with 4 hex characters
    Float32Ieee(4), // Assuming you represent Float32 with 8 hex characters
    Float64Ieee(8), // Assuming you represent Float64 with 16 hex characters
    Default(1),
    NOT_SET(0);
}

/**
 * Parses a string and returns the DataType if it contains one, else returns default value of 1
  */
fun String.parseDataTypeEnum(): DataType {
    when (this.trim()) {
        "UBYTE" -> {
            return DataType.UBYTE
        }
        "SBYTE" -> {
            return DataType.SBYTE
        }
        "UWORD" -> {
            return DataType.UWORD
        }
        "SWORD" -> {
            return DataType.SWORD
        }
        "ULONG" -> {
            return DataType.ULONG
        }
        "SLONG" -> {
            return DataType.SLONG
        }
        "AUint64" -> {
            return DataType.AUint64
        }
        "AInt64" -> {
            return DataType.AInt64
        }
        "Float16Ieee" -> {
            return DataType.Float16Ieee
        }
        "Float32Ieee" -> {
            return DataType.Float32Ieee
        }
        "Float64Ieee" -> {
            return DataType.Float64Ieee
        }
        else -> {
            return DataType.Default
        }
    }
}