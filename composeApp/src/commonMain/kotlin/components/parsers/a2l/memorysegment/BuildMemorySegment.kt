package components.parsers.a2l.memorysegment

import components.parsers.a2l.classes.enums.parseAttribute
import components.parsers.a2l.classes.enums.parseMemorySegmentProgramType
import components.parsers.a2l.classes.enums.parseMemoryType
import components.a2l.ifdata.components.parseIfDataSection

fun buildMemorySegment(input: List<String>): MemorySegment {
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

    // Check if size is correct, else we throw index out of bounds
    if (list.size >= 12) {
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

        return MemorySegment(
            name = list[0].trim(),
            longIdentifier = list[1].trim(),
            prgType = list[2].parseMemorySegmentProgramType(),
            memoryType = list[3].parseMemoryType(),
            attribute = list[4].parseAttribute(),
            address = list[5].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
            size = list[6].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
            offset = mOffset,
            ifData = mIfData
        )
    } else return MemorySegment()
}