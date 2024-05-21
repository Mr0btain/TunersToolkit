package screens.editor.components.ascii

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class AsciiVisualTransformation(private val n: Int) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        val outputBuilder = StringBuilder()

        for ((i, c) in input.withIndex()) {
            val isAsciiLetterOrDigit = c.isLetterOrDigit() && c.code < 128
            outputBuilder.append(if (isAsciiLetterOrDigit) c else '.')

            if ((i + 1) % n == 0 && i < input.length - 1) {
                outputBuilder.append('\n')
            }
        }

        val output = outputBuilder.toString()

        return TransformedText(AnnotatedString(output), AsciiOffsetMapper(output))
    }

    internal class AsciiOffsetMapper(private val transformed: String) : OffsetMapping {
        private val mLineLength = transformed.lineSequence().first().replace("\n", "").length

        override fun originalToTransformed(offset: Int): Int {
            var position = 0
            var shift = 0

            while (position < offset) {
                if (position == 0) {
                    position += mLineLength
                } else {
                    position += mLineLength
                    shift++
                }
            }

            return offset + shift
        }

        override fun transformedToOriginal(offset: Int): Int {
            // Get offset minus all newline Characters
            return offset - transformed.subSequence(0, offset).count { it == '\n' }
        }
    }
}