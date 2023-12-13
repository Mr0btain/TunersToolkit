package components.parsers.a2l.classes.enums

import kotlinx.serialization.Serializable

/**
 * Description of the type of memory used
 * @property EEPROM Segment of EEPROM
 * @property EPROM Segment of EPROM
 * @property FLASH Segment of FLASH
 * @property RAM Segment of RAM
 * @property ROM Segment of ROM
 * @property REGISTER Segment of CPU registers
 */
@Serializable
enum class MemoryType {
    EEPROM,
    EPROM,
    FLASH,
    RAM,
    ROM,
    REGISTER,
    NOT_IN_ECU,
    NOT_SET;
}

fun String.parseMemoryType(): MemoryType {
    return when (this.trim()) {
        "EEPROM" -> MemoryType.EEPROM
        "EPROM" -> MemoryType.EPROM
        "FLASH" -> MemoryType.FLASH
        "RAM" -> MemoryType.RAM
        "ROM" -> MemoryType.ROM
        "REGISTER" -> MemoryType.REGISTER
        "NOT_IN_ECU" -> MemoryType.NOT_IN_ECU
        else -> MemoryType.NOT_SET
    }
}