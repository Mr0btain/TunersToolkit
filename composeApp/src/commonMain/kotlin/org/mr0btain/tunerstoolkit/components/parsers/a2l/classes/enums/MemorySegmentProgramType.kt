package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import kotlinx.serialization.Serializable

/**
 * @property CALIBRATION_VARIABLES Values which are available in the ECU but do
 * not exist in the Hex-file. There is no upload
 * required to get access to the ECU data. The ECU
 * will never be touched by the instrumentation tool
 * except by upload.
 * @property CODE Program code
 * @property DATA Program data allowed for online
 * calibration
 * @property EXCLUDE_FROM_FLASH Values existing in the ECU but not dropped down
 * in the binary file. There should no upload be
 * needed to get access to the ECU data. The ECU
 * will never be touched by the instrumentation tool
 * except by upload.
 * @property OFFLINE_DATA Program data allowed only for offline calibration
 * @property RESERVED Reserved segments
 * @property SERAM Program data for serial emulation
 * @property VARIABLES Program variables
 * @property NOT_SET No value has been set, placeholder essentially
 */
@Serializable
enum class MemorySegmentProgramType {
    CALIBRATION_VARIABLES,
    CODE,
    DATA,
    EXCLUDE_FROM_FLASH,
    OFFLINE_DATA,
    RESERVED,
    SERAM,
    VARIABLES,
    NOT_SET;
}

fun String.parseMemorySegmentProgramType(): MemorySegmentProgramType {
    return when (this.trim()) {
        "CALIBRATION_VARIABLES" -> MemorySegmentProgramType.CALIBRATION_VARIABLES
        "CODE" -> MemorySegmentProgramType.CODE
        "DATA" -> MemorySegmentProgramType.DATA
        "EXCLUDE_FROM_FLASH" -> MemorySegmentProgramType.EXCLUDE_FROM_FLASH
        "OFFLINE_DATA" -> MemorySegmentProgramType.OFFLINE_DATA
        "RESERVED" -> MemorySegmentProgramType.RESERVED
        "SERAM" -> MemorySegmentProgramType.SERAM
        "VARIABLES" -> MemorySegmentProgramType.VARIABLES
        else -> MemorySegmentProgramType.NOT_SET
    }
}