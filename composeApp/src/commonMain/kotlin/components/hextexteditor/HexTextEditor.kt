@file:Suppress("FunctionName")

package components.hextexteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import components.hextexteditor.components.getTextAlignmentLine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * A Custom made Hex Editor to handle large amounts of data that need to be represented in
 * a certain format. Easier and way more performant to have a custom implementation than to
 * work around TextFields constraints.
 * @param modifier The Modifier, applied to the BoxWithConstraints surrounding the canvas
 * @param input The input to display
 */

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HexTextEditor(
    modifier: Modifier,
    input: String,
    bits: Int,
    digitsPerLine: Int,
    textStyle: TextStyle,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val charactersToPull = "abcdefghijklmnopqrstuvwxyz0123456789"
    val characterPainters = mutableMapOf<Char, Painter>()
    val density = LocalDensity.current.density

    println("Font size desired: ${textStyle.fontSize}")
    println("Font Scale as per density: $density")

    // Get the actual objects to paint
    charactersToPull.forEach {
        characterPainters[it] = painterResource("svg/$it.xml")
    }

    BoxWithConstraints {
        // Calculate size to paint the text based on screen width, and density
        // using screen width this loosely gives us an auto resize text
        val textHeight = ((maxWidth / 440) * textStyle.fontSize.value * density).value
        val textWidth = (textHeight / 2.375).toFloat()
        val textBeginAlignmentLine = getTextAlignmentLine(textWidth, digitsPerLine, bits, maxWidth.value)

        println("Font Height Calc: $textHeight")
        println("Font width calc: $textWidth")

        Canvas(
            modifier = modifier
        ) {
            var xPosition = textBeginAlignmentLine
            var yPosition = 0f

            input.forEachIndexed { index, char ->
                // Check if we need to start a new line
                if (index % digitsPerLine == 0) {
                    xPosition = textBeginAlignmentLine
                    yPosition += (textHeight + 2)
                }

                // Check if we need to add a space
                if (index % bits == 0) { xPosition += textWidth - 2 }

                // Paint the character on screen at the position
                translate(top = yPosition, left = xPosition) {
                    val objectToPaint = characterPainters[char.lowercaseChar()]

                    if (objectToPaint != null) {
                        with(objectToPaint) {
                            draw(
                                size = Size(width = textWidth, height = textHeight),
                                colorFilter = ColorFilter.tint(color = color)
                            )
                        }
                    }
                }

                // advance the xPosition
                xPosition += textWidth
            }
        }
    }
}