package components.parsers.a2l.classes.enums

import kotlinx.serialization.Serializable

/**
 * Enumeration for description of the word lengths in the ECU program
 */

@Serializable
enum class DataSize {
    BYTE,
    WORD,
    LONG,
    NOT_SET
}

fun String.parseDataSize(): DataSize {
    return when (this.replace("DATA_SIZE", "").trim()) {
        "BYTE" -> DataSize.BYTE
        "WORD" -> DataSize.WORD
        "LONG" -> DataSize.LONG
        else -> DataSize.NOT_SET
    }
}