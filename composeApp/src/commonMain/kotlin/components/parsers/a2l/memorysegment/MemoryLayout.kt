package components.parsers.a2l.memorysegment

import components.parsers.a2l.classes.data.MemoryLayoutProgramType
import components.parsers.a2l.classes.data.parseMemLayoutPrgType
import components.a2l.ifdata.components.parseIfDataSection
import kotlinx.serialization.Serializable


/**
 * This data record is used to describe an ECU program. The description indicates how
 * the emulation memory is divided into the individual segments.
 * @param prgType Description of the program segments
 * @param address Initial address of the program segment to be described.
 * @param size Length of the program segment to be described.
 * @param offset In special ECU programs, so-called 'mirrored
 * segments' may occur. A mirrored segment is a copy of
 * another program segment. During adjustment the data
 * changes are introduced in the relevant memory
 * segment as well as in all mirrored segments.
 * @param ifData Data record to describe interface specific data of the
 * memory layout.
 */
@Serializable
data class MemoryLayout(
    val prgType: MemoryLayoutProgramType = MemoryLayoutProgramType.NOT_SET,
    val address: ULong = ULong.MIN_VALUE,
    val size: ULong = ULong.MIN_VALUE,
    val offset: List<Long> = listOf(),
    val ifData: List<components.parsers.a2l.ifdata.IfData> = listOf()
)

fun MemoryLayout.isNotEmpty(): Boolean {
    return when {
        prgType == MemoryLayoutProgramType.NOT_SET -> false
        address == ULong.MIN_VALUE -> false
        else -> true
    }
}

fun List<String>.parseMemoryLayout(): MemoryLayout {
    // Check if size it big enough else we get index out of bounds
    if (this.size >= 4) {
        var mPrgType = MemoryLayoutProgramType.NOT_SET
        val mOffset = mutableListOf<Long>()
        val mIfData = mutableListOf<components.parsers.a2l.ifdata.IfData>()

        this.forEachIndexed {index, line ->
            when {
                line.startsWith("/begin MEMORY_LAYOUT") -> {
                    val result = line.replace("/begin MEMORY_LAYOUT", "").trim()
                    if (result.isNotBlank()) {
                        mPrgType = result.parseMemLayoutPrgType()
                    }
                }

                line.startsWith("/begin IF_DATA") -> {
                    mIfData.add(this.subList(index, this.size).parseIfDataSection())
                }
            }
        }

        if (mPrgType != MemoryLayoutProgramType.NOT_SET) {
            val result = this[3].trim().split(" ")
            if (result.size >= 5) {
                result.forEach {
                    if (it.replace("0x", "").toULongOrNull(16) != null) {
                        mOffset.add(it.toLong())
                    }
                }
            }
        } else {
            val result = this[4].trim().split(" ")
            if (result.size >= 5) {
                result.forEach {
                    if (it.replace("0x", "").toULongOrNull(16) != null) {
                        mOffset.add(it.toLong())
                    }
                }
            }
        }

        return if (mPrgType != MemoryLayoutProgramType.NOT_SET) {
            MemoryLayout(
                prgType = mPrgType,
                address = this[1].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                size = this[2].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                offset = mOffset,
                ifData = mIfData
            )
        } else {
            MemoryLayout(
                prgType = this[1].parseMemLayoutPrgType(),
                address = this[2].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                size = this[3].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                offset = mOffset,
                ifData = mIfData
            )
        }
    } else {
        return MemoryLayout()
    }
}