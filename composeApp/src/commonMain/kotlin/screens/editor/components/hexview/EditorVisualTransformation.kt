package screens.editor.components.hexview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class EditorVisualTransformation(
    private val digitsPer: Int
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        val output = buildString {
            for (i in input.indices) {
                if (i % 2 == 0 && i != 0) {
                    append(' ')
                }
                if (i % (2 * digitsPer) == 0 && i != 0) {
                    append('\n')
                }
                append(input[i])
            }
        }
        return TransformedText(AnnotatedString(output), HexOffsetMapping(output, digitsPer, text.length)) // EditorOffsetMap(output)
    }

    internal class HexOffsetMapping(private val transformed: String, private val digitsPer: Int, private val originalLength: Int) : OffsetMapping {
        private var mBeforeIsSpace = false
         override fun originalToTransformed(offset: Int): Int {
            var mShift = 0

            for (i in 0..offset) {
                if (i % 2 == 0 && i != 0) {
                    mShift++
                }
                if (i % (2 * digitsPer) == 0 && i != 0) {
                    mShift++
                }
            }

            return when (offset) {
                0 -> 0

                originalLength -> transformed.length

                else -> {
                    val mNewOffset = offset + mShift
                    val mLineLnegth = transformed.lineSequence().first().length
                    val mChar = transformed[mNewOffset]

                    if (!mBeforeIsSpace && mNewOffset % 3 != 0 && offset % 2 == 0) {
                        if (transformed[mNewOffset - 1] == '\n') {
                            mNewOffset
                        } else mNewOffset - 1
                    } else mNewOffset
                }
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            mBeforeIsSpace = transformed[offset - 1] == ' '

            return offset - transformed.subSequence(0, offset).count { it == '\n' || it == ' ' }
        }
    }
}