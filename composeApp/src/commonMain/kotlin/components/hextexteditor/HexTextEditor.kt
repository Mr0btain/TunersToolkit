@file:Suppress("FunctionName")

package components.hextexteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import components.hextexteditor.components.HexTextPositionHandler
import components.hextexteditor.components.getTextAlignmentLine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * A Custom made Hex Editor to handle large amounts of data that need to be represented in
 * a certain format. Easier and way more performant to have a custom implementation than to
 * work around TextFields constraints.
 * @param modifier The Modifier, applied to the BoxWithConstraints surrounding the canvas
 * @param input The input to display
 * @param bits The number of hex digits we should display, ex 2 4 6
 * @param digitsPerLine The max number of digits per line
 * @param textStyle How the text should be styled
 * @param color The color of the text. should be moved to textStyle
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
    val cursorPainter = painterResource("svg/i.xml")
    val cursorPosition = remember { mutableStateOf(Offset(0f,0f)) }
    val isCursorVisible = remember { mutableStateOf(false) }
    val textPositionHandler = HexTextPositionHandler()

    println("Font size desired: ${textStyle.fontSize}")
    println("Font Scale as per density: $density")
    println("isCursorVisible: ${isCursorVisible.value}")
    println("Cursor Offset: ${cursorPosition.value}")

    // Get the actual objects to paint
    charactersToPull.forEach {
        characterPainters[it] = painterResource("svg/$it.xml")
    }

    BoxWithConstraints {
        // Calculate size to paint the text based on screen width, and density
        // using screen width this loosely gives us an auto resize text
        val textHeight = ((maxWidth / 440) * textStyle.fontSize.value * density).value
        val textWidth = (textHeight / 2.375).toFloat()
        val textAlignmentLine = getTextAlignmentLine(textWidth, digitsPerLine, bits, maxWidth.value)

        // Set character data in position handler
        textPositionHandler.addCharacterData(
            characterSize = Size(width = textWidth, height = textHeight),
            bits = bits,
            charactersPerLine = digitsPerLine
        )

        var lineNum = 0
        var xLinePosition = textAlignmentLine
        var yLinePosition = 0f

        // Setup Line data in position handler
        input.forEachIndexed { index, char ->
            // Check if we need to start a new line
            if (index % digitsPerLine == 0 && index != 0) {
                textPositionHandler.addLineData(
                    lineNumber = lineNum,
                    lineStartOffset = textAlignmentLine,
                    lineTopOffset = yLinePosition,
                    lineEndOffset = xLinePosition
                )

                lineNum++
                xLinePosition = textAlignmentLine
                yLinePosition += (textHeight + 2)
            }

            // Check if we need to add a space
            if (index % bits == 0) {
                xLinePosition += textWidth
            }

            if (index == input.length - 1) {
                textPositionHandler.addLineData(
                    lineNumber = lineNum,
                    lineStartOffset = textAlignmentLine,
                    lineTopOffset = yLinePosition,
                    lineEndOffset = xLinePosition + textWidth
                )
            }

            // advance the xPosition
            xLinePosition += textWidth
        }

        println("Font Height Calc: $textHeight")
        println("Font width calc: $textWidth")

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            println("Pointer Offset: $offset")
                            println("X Start line: $textAlignmentLine")
                            println("Text right end: ${textAlignmentLine + (textWidth * digitsPerLine)}")

                            val characterPos = textPositionHandler.getCharacterRectFromOffset(offset)

                            cursorPosition.value = Offset(x = characterPos.right, y = characterPos.top)

                            if (cursorPosition.value.x == 0f && cursorPosition.value.y == 0f) {
                                isCursorVisible.value = false
                            } else isCursorVisible.value = true

                        }
                    )
                }
        ) {
            var xPosition = textAlignmentLine
            var yPosition = 0f

            if (input.isNotEmpty()) {
                println("Painting Text")
                input.forEachIndexed { index, char ->
                    // Check if we need to start a new line
                    if (index % digitsPerLine == 0 && index != 0) {
                        xPosition = textAlignmentLine
                        yPosition += (textHeight + 2)
                    }

                    // Check if we need to add a space
                    if (index % bits == 0) {
                        xPosition += textWidth
                    }

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

            if (isCursorVisible.value) {
                println("Painting cursor")

                drawLine(
                    color = Color.Red,
                    start = Offset(x = cursorPosition.value.x, y = cursorPosition.value.y + 3f),
                    end =  Offset(x = cursorPosition.value.x, y = cursorPosition.value.y + textHeight - 3f),
                    strokeWidth = 2f
                )
            }
        }
    }
}