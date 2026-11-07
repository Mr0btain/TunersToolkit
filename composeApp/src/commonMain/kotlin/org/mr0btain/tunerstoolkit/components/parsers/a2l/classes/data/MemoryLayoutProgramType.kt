package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import kotlinx.serialization.Serializable

/**
 * Description of the program segments divided into:
 * PRG_CODE = program code
 * PRG_DATA = program data
 * PRG_RESERVED = other
 */
@Serializable
enum class MemoryLayoutProgramType {
    PRG_CODE,
    PRG_DATA,
    PRG_RESERVED,
    NOT_SET;
}

fun String.parseMemLayoutPrgType(): MemoryLayoutProgramType {
    return when (this.replace("/begin MEMORY_LAYOUT", "").trim()) {
        "PRG_CODE" -> MemoryLayoutProgramType.PRG_CODE
        "PRG_DATA" -> MemoryLayoutProgramType.PRG_DATA
        "PRG_RESERVED" -> MemoryLayoutProgramType.PRG_RESERVED
        else -> MemoryLayoutProgramType.NOT_SET
    }
}