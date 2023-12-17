package components.parsers.a2l.memorysegment

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.enums.parseAttribute
import components.parsers.a2l.classes.enums.parseMemorySegmentProgramType
import components.parsers.a2l.classes.enums.parseMemoryType
import components.parsers.a2l.ifdata.components.parseIfDataSection

fun buildMemorySegment(input: List<String>): Pair<MemorySegment, List<LogDataEntry>> {
    val process = "BuildMemorySegment"
    val errors = mutableListOf<LogDataEntry>()

    safeCall {
        // Trim up first item in case name lies on first line
        val list = input[0].replace("/begin MEMORY_SEGMENT", "")
            .trim().split(" ").toMutableList()

        // If separated line is not blank, then it did contain the name of the segment
        if (list[0].isNotBlank()) {
            // add the rest of the segment
            list.addAll(input.subList(1, input.size))
        } else {
            // Line did not contain anything, therefore name is on next line so negate that item
            list.clear()
            list.addAll(input.subList(1, input.size))
        }

        val mOffset = mutableListOf<Long>()
        val mIfData = mutableListOf<components.parsers.a2l.ifdata.IfData>()

        list.subList(7,12).forEach {
            val i = it.toLongOrNull()
            if (i != null) mOffset.add(i)
        }

        if (list.size > 11) {
            list.forEachIndexed { index, line ->
                when {
                    (line.startsWith("/begin IF_DATA")) -> {
                        mIfData.add(list.subList(index, list.size).parseIfDataSection())
                    }
                }
            }
        }

        return Pair(
            MemorySegment(
                name = list[0].trim(),
                longIdentifier = list[1].trim(),
                prgType = list[2].parseMemorySegmentProgramType(),
                memoryType = list[3].parseMemoryType(),
                attribute = list[4].parseAttribute(),
                address = list[5].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                size = list[6].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                offset = mOffset,
                ifData = mIfData
            ),
            errors
        )
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to build MemorySegment: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(MemorySegment(), errors)
}