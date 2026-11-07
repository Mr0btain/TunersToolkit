package org.mr0btain.tunerstoolkit.components.maptextoverlay.classes
import androidx.compose.ui.text.input.OffsetMapping

class OffsetMapper(private val input: String): OffsetMapping {
    private var offsetMap = mapOf<Int, Int>()

    init {
        generateOffsetMap(input.length)
    }

    override fun originalToTransformed(offset: Int): Int {
        return if (offsetMap.keys.last() >= offset) {
            offsetMap[offset] ?: 0
        } else offsetMap.values.last()
    }

    override fun transformedToOriginal(offset: Int): Int {
        val chars = input.subSequence(if (offset <= 0) offset else offset - 1, offset + 1)

        // If the Character before is a new line character, then we need to move the offset position back
        // two spaces to get a result, else the offset map may not contain our offset, for example
        // if the cursor is: (02 \n|03 04) then the legit position in the map would be (02| \n03 04)
        if (chars[0] == '\n') {
            offsetMap.forEach { (key, value) ->
                if (value == offset - 2) {
                    return key
                }
            }
        } else if (chars[0] == ' ') {
            // Else we only need to remove one from offset
            offsetMap.forEach { (key, value) ->
                if (value == offset - 1) {
                    return key
                }
            }
        } else {
            var lastKey = 0
            var lastOffset = 0

            offsetMap.forEach { (key, value) ->
                if (value == offset) {
                    return key
                } else if (offset in lastOffset..value) {
                    return lastKey
                }

                lastKey = key
                lastOffset = value
            }
        }

        return 0
    }

    private fun generateOffsetMap(length: Int) {
        val result = mutableMapOf<Int, Int>()
        var transformedAmount = 0
        var current = 0

        for (i in 0 until length) {
            // For every pair of hex digits, add one for the space
            if (current % 2 == 0 && current != 0) {
                transformedAmount++
                result[current] = current + transformedAmount
            }

            // For every line, add one for the \n
            if (current % 16 == 0 && current != 0) {
                current++
                transformedAmount++
                result[current] = current + transformedAmount
            }

            current++
            //result[i] = i + transformedAmount
        }

        offsetMap = result
    }
}