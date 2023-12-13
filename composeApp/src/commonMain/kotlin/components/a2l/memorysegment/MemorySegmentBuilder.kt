package components.a2l.memorysegment

import components.a2l.classes.enums.parseAttribute
import components.a2l.classes.enums.parseMemorySegmentProgramType
import components.a2l.classes.enums.parseMemoryType
import components.a2l.ifdata.IfData
import components.a2l.ifdata.components.parseIfDataSection

class MemorySegmentBuilder {

    fun build(input: List<String>): MemorySegment {
        // Trim up first item in case name lies on first line
        var list = input[0].replace("/begin MEMORY_SEGMENT", "")
            .trim().split(" ").toMutableList()

        // If separated line is not blank, then it did contain the name of the segment
        // add the rest of the segment
        if (list[0].isNotBlank()) {
            input.subList(1, input.size).forEach {
                list.add(it.trim())
            }
        } else {
            // Line did not contain anything, therefore name is on next line so negate that item
            list = input.subList(1, input.size).toMutableList()
            }

        return buildObject(list)
    }

    private fun buildObject(input: List<String>): MemorySegment {
        // Check if size is correct, else we throw index out of bounds
        if (input.size >= 12) {
            val mOffset = mutableListOf<Long>()
            val mIfData = mutableListOf<IfData>()

            input.subList(7,12).forEach {
                val i = it.toLongOrNull()
                if (i != null) mOffset.add(i)
            }

            if (input.size > 11) {
                input.forEachIndexed { index, line ->
                    when {
                        (line.startsWith("/begin IF_DATA")) -> {
                            mIfData.add(input.subList(index, input.size).parseIfDataSection())
                        }
                    }
                }
            }

            return MemorySegment(
                name = input[0].trim(),
                longIdentifier = input[1].trim(),
                prgType = input[2].parseMemorySegmentProgramType(),
                memoryType = input[3].parseMemoryType(),
                attribute = input[4].parseAttribute(),
                address = input[5].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                size = input[6].replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE,
                offset = mOffset,
                ifData = mIfData
            )
        } else return MemorySegment()
    }
}