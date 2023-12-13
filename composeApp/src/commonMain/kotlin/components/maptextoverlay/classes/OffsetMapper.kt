package components.maptextoverlay.classes

import androidx.compose.ui.text.input.OffsetMapping

class OffsetMapper(private val input: String): OffsetMapping {
    private var mOffsetMap = mapOf<Int, Int>()

    init {
        generateOffsetMap(input.length)
    }

    override fun originalToTransformed(offset: Int): Int {
        return if (mOffsetMap.keys.last() >= offset) {
            mOffsetMap[offset] ?: 0
        } else mOffsetMap.values.last()
    }

    override fun transformedToOriginal(offset: Int): Int {
        val mChars = input.subSequence(if (offset <= 0) offset else offset - 1, offset + 1)

        // If the Character before is a new line character, then we need to move the offset position back
        // two spaces to get a result, else the offset map may not contain our offset, for example
        // if the cursor is: (02 \n|03 04) then the legit position in the map would be (02| \n03 04)
        if (mChars[0] == '\n') {
            mOffsetMap.forEach { (key, value) ->
                if (value == offset - 2) {
                    return key
                }
            }
        } else if (mChars[0] == ' ') {
            // Else we only need to remove one from offset
            mOffsetMap.forEach { (key, value) ->
                if (value == offset - 1) {
                    return key
                }
            }
        } else {
            var mLastKey = 0
            var mLastOffset = 0

            mOffsetMap.forEach { (key, value) ->
                if (value == offset) {
                    return key
                } else if (offset in mLastOffset..value) {
                    return mLastKey
                }

                mLastKey = key
                mLastOffset = value
            }
        }

        return 0
    }

    private fun generateOffsetMap(length: Int) {
        val mResult = mutableMapOf<Int, Int>()
        var transformedAmount = 0
        var current = 0

        for (i in 0 until length) {
            // For every pair of hex digits, add one for the space
            if (current % 2 == 0 && current != 0) {
                transformedAmount++
                mResult[current] = current + transformedAmount
            }

            // For every line, add one for the \n
            if (current % 16 == 0 && current != 0) {
                current++
                transformedAmount++
                mResult[current] = current + transformedAmount
            }

            current++
            //mResult[i] = i + transformedAmount
        }

        mOffsetMap = mResult
    }
}