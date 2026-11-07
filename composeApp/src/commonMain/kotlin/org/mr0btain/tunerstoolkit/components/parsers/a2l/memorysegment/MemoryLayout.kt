package org.mr0btain.tunerstoolkit.components.parsers.a2l.memorysegment
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MemoryLayoutProgramType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseMemLayoutPrgType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.parseIfDataSection
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
    val ifData: List<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData> = listOf()
)

fun MemoryLayout.isNotEmpty(): Boolean {
    return when {
        prgType == MemoryLayoutProgramType.NOT_SET -> false
        address == ULong.MIN_VALUE -> false
        else -> true
    }
}

fun List<String>.parseMemoryLayout(): Pair<MemoryLayout, List<LogDataEntry>> {
    val process = "ParseMemoryLayout"
    val errors = mutableListOf<LogDataEntry>()
    var prgType = MemoryLayoutProgramType.NOT_SET
    val offset = mutableListOf<Long>()
    val ifData = mutableListOf<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData>()

    this.forEachIndexed {index, line ->
        when {
            line.startsWith("/begin MEMORY_LAYOUT") -> {
                safeCall {
                    prgType = line.replace("/begin MEMORY_LAYOUT", "")
                        .trim().parseMemLayoutPrgType()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse Prg type!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin IF_DATA") -> {
                safeCall {
                    ifData.add(this.subList(index, this.size).parseIfDataSection())
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add IfData!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }
        }
    }

    safeCall {
        if (prgType != MemoryLayoutProgramType.NOT_SET) {
            val result = this[3].trim().split(" ")

            result.forEach {
                offset.add(it.replace("0x", "").toLong(16))
            }
        } else {
            val result = this[4].trim().split(" ")

            result.forEach {
                offset.add(it.replace("0x", "").toLong(16))
            }
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to add Offset!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    safeCall {
        return if (prgType != MemoryLayoutProgramType.NOT_SET) {
            Pair(
                MemoryLayout(
                    prgType = prgType,
                    address = this[1].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                    size = this[2].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                    offset = offset,
                    ifData = ifData
                ),
                errors
            )
        } else {
            Pair(
                MemoryLayout(
                    prgType = this[1].parseMemLayoutPrgType(),
                    address = this[2].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                    size = this[3].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                    offset = offset,
                    ifData = ifData
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to build MemoryLayout!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(MemoryLayout(), errors)
}