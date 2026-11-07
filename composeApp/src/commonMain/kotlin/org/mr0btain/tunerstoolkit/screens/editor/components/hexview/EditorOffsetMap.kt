package org.mr0btain.tunerstoolkit.screens.editor.components.hexview
import androidx.compose.ui.text.input.OffsetMapping

class EditorOffsetMap(private val transformed: String) : OffsetMapping {
    private fun isDivider(char: Char): Boolean {
        return char == ' ' || char == '\n'
    }

    override fun originalToTransformed(offset: Int): Int {
        val transformedOffsets = transformed
            .mapIndexedNotNull { index, c ->
                index
                    .takeIf { !isDivider(c) }
                    // convert index to a usable offset
                    ?.plus(1)
            }
            // Now we need to have an offset of 0, as the cursor
            // starts at the left of the first char and is designated as 0
            .let { offsetList ->
                listOf(0) + offsetList
            }
        return transformedOffsets[offset]
    }

    override fun transformedToOriginal(offset: Int): Int =
        transformed
            // This creates a list of all separator offsets
            .mapIndexedNotNull { index, c ->
                index.takeIf { isDivider(c) }
            }
            // We want to count how many separators precede the transformed offset
            .count { separatorIndex ->
                separatorIndex < offset
            }
            // We find the original offset by subtracting the number of separators
            .let { separatorCount ->
                offset - separatorCount
            }
}