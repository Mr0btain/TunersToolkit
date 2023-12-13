package components.a2l.classes.enums

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class ByteOrder : A2lOptional {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

fun String.parseByteOrder(): ByteOrder {
    return when (this.replace("BYTE_ORDER", "").trim()) {
        "LITTLE_ENDIAN" -> ByteOrder.LITTLE_ENDIAN
        "BIG_ENDIAN" -> ByteOrder.BIG_ENDIAN
        "MSB_LAST" -> ByteOrder.LITTLE_ENDIAN
        "MSB_FIRST" -> ByteOrder.BIG_ENDIAN
        else -> ByteOrder.LITTLE_ENDIAN
    }
}