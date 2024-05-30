package components.hextexteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HexTextEditor(
    modifier: Modifier,
    input: String,
    bits: Int,
    digitsPerLine: Int,
    textStyle: TextStyle
) {
    val charsToGet = "abcdefghijklmnopqrstuvwxyz0123456789"
    val chars = mutableMapOf<Char, Painter>()
    val fontScale = LocalDensity.current.density

    println("Font size desired: ${textStyle.fontSize}")
    println("Font Scale as per density: $fontScale")

    // Get the actual objects to paint
    charsToGet.forEach {
        chars[it] = painterResource("svg/$it.xml")
    }

    BoxWithConstraints {
        // Calculate size to paint the text based on screen width, and density
        // using screen width this loosely gives us an auto resize text
        val textHeight = ((maxWidth / 440) * textStyle.fontSize.value * fontScale).value
        val textWidth = (textHeight / 2.375).toFloat()

        println("Font Height Calc: $textHeight")
        println("Font width calc: $textWidth")

        Canvas(
            modifier = modifier
        ) {
            var xPosition = 0f
            var yPosition = 0f

            input.forEachIndexed { index, char ->
                // Check if we need to start a new line
                if (index % digitsPerLine == 0) {
                    yPosition += (textHeight + 2)
                    xPosition = 0f
                }

                // Check if we need to add a space
                if (index % bits == 0) { xPosition += textWidth - 2 }

                // Paint the character on screen at the position
                translate(top = yPosition, left = xPosition) {
                    val objectToPaint = chars[char.lowercaseChar()]

                    if (objectToPaint != null) {
                        with(objectToPaint) {
                            draw(Size(width = textWidth, height = textHeight))
                        }
                    }
                }

                // advance the xPosition
                xPosition += textWidth
            }
        }
    }
}