package components.a2l.memorysegment

import components.a2l.classes.enums.Attribute
import components.a2l.classes.enums.MemorySegmentProgramType
import components.a2l.classes.enums.MemoryType
import components.a2l.ifdata.IfData
import kotlinx.serialization.Serializable

/**
 * The new keyword MEMORY_SEGMENT is used to replace the existing keyword
 * MEMORY_LAYOUT. The advantages of MEMORY_SEGMENT are that they are given
 * a name which can be used for references from IF_DATA Blobs and the more accurate
 * description of the memory by memory types and attributes (INTERN and EXTERN).
 * The keywords MEMORY_SEGMENT and MEMORY_LAYOUT can be used in parallel.
 * The parameter Offset is to be used (as within the former MEMORY_LAYOUT) to
 * describe several mirrored segments.
 * MEMORY_SEGMENTS with the same MemoryType and the same Attribute may not
 * overlap. Also, all MEMORY_SEGMENTS with the PrgType CODE, DATA,
 * OFFLINE_DATA, RESERVED may not overlap mutually to get a linear address space
 * for access on calibration data. All other MEMORY_SEGMENTS with different
 * MemoryType or different Attribute may however overlap, e.g. internal and external
 * memory segments.
 *@param name Unique identifier
 * @param longIdentifier detailed description
 * @param prgType
 */

@Serializable
data class MemorySegment(
    var name: String = "",
    val longIdentifier: String = "",
    val prgType: MemorySegmentProgramType = MemorySegmentProgramType.NOT_SET,
    val memoryType: MemoryType = MemoryType.NOT_SET,
    val attribute: Attribute = Attribute.NOT_SET,
    val address: ULong = ULong.MIN_VALUE,
    val size: ULong = ULong.MIN_VALUE,
    val offset: List<Long> = listOf(),
    val ifData: List<IfData> = listOf()
)