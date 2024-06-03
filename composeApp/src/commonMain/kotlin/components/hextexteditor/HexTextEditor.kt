@file:Suppress("FunctionName")

package components.hextexteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.forEachChange
import androidx.compose.foundation.text2.input.rememberTextFieldState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import components.hextexteditor.components.HexTextEditorManager
import components.hextexteditor.components.getTextAlignmentLine
import components.hextexteditor.components.hexTextOnClick
import components.hextexteditor.components.mapLineRectData
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import screens.editor.components.hexview.EditorOnValueChanged
import screens.editor.components.hexview.EditorVisualTransformation
import tunerstoolkit.composeapp.generated.resources.*

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
    val characterPainters = remember { mutableMapOf<Char, Painter>() }
    val density = LocalDensity.current.density
    val cursorPosition = remember { mutableStateOf(Offset(0f,0f)) }
    val isCursorVisible = remember { mutableStateOf(false) }
    val isCursorDrawn = mutableStateOf(false)
    val manager = HexTextEditorManager()

    getHexCharacterRes().forEach { (c, painter) ->
        characterPainters[c] = painter
    }

    // If cursor is to be shown, this logic makes it blink
    LaunchedEffect(isCursorVisible.value) {
        while (isCursorVisible.value) {
            isCursorDrawn.value = !isCursorDrawn.value
            delay(1000)
        }
    }

    BoxWithConstraints {
        // Calculate size to paint the text based on screen width, and density
        // using screen width this loosely gives us an auto resize text
        val textHeight = ((maxWidth / 440) * textStyle.fontSize.value * density).value
        val textWidth = (textHeight / 2.375).toFloat()
        val textAlignmentLine = getTextAlignmentLine(textWidth, digitsPerLine, bits, maxWidth.value)

        // Only run these when something changes with the character sizes
        LaunchedEffect(key1 = textHeight) {
            // Set character data in position handler
            manager.addCharacterData(
                characterSize = Size(width = textWidth, height = textHeight),
                bits = bits,
                charactersPerLine = digitsPerLine
            )

            // Map out line rect data
            mapLineRectData(
                input = input,
                textWidth = textWidth,
                textHeight = textHeight,
                textAlignmentLine = textAlignmentLine,
                digitsPerLine = digitsPerLine,
                bits = bits,
                textPositionHandler = manager
            )
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .hexTextOnClick(
                    manager = manager,
                    isCursorVisible = isCursorVisible,
                    cursorPosition = cursorPosition
                )
        ) {
            var xPosition = textAlignmentLine
            var yPosition = 0f

            if (input.isNotEmpty()) {
                input.forEachIndexed { index, char ->
                    // Check if we need to start a new line or a spacer
                    if (index % digitsPerLine == 0 && index != 0) {
                        xPosition = textAlignmentLine
                        yPosition += (textHeight + 2)
                    } else if (index % bits == 0 && xPosition != textAlignmentLine) {
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

                    // Advance the xPosition
                    xPosition += textWidth
                }
            }

            if (isCursorDrawn.value) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexTextEditor2(
    input: String,
    textStyle: TextStyle
) {
    val testInput = input + input + input + input + input + input + input + input + input + input + input + input
    val textValue = remember { mutableStateOf(TextFieldValue(testInput)) }
    val textFont = FontFamily(Font(Res.font.jbmono_reg))
    val textState = rememberTextFieldState(initialText = input)

    BasicTextField2(
        state = textState,
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontFamily = textFont
        ),
        inputTransformation = { originalValue, valueWithChanges ->
            valueWithChanges.changes.forEachChange { new, orig ->
                println(orig)
                println(new)

                if (new.start == new.end) {
                    valueWithChanges.revertAllChanges()
                }
            }

            /*
            when {
                newLength > oldLength -> {
                    when {
                        valueWithChanges.selectionInChars.start <= oldLength -> {
                            val input = valueWithChanges.changes.
                        }

                        newTextField.selection.start <= oldLength -> {
                            val inputChar = newTextField.text[newTextField.selection.start - 1].uppercaseChar()

                            when {
                                inputChar.isDigit() || inputChar in setOf('A', 'B', 'C', 'D', 'E', 'F') -> {
                                    TextFieldValue(
                                        text = oldTextField.text.replaceRange(
                                            newTextField.selection.start - 1,
                                            newTextField.selection.end,
                                            inputChar.toString()
                                        ),
                                        selection = newTextField.selection
                                    )
                                }

                                else -> {
                                    TextFieldValue(
                                        text = oldTextField.text,
                                        selection = TextRange(
                                            newTextField.selection.start - 1,
                                            newTextField.selection.end - 1
                                        ),
                                    )
                                }
                            }
                        }

                        else -> {
                            TextFieldValue(
                                text = oldTextField.text,
                                selection = newTextField.selection
                            )
                        }
                    }
                }

                newLength < oldLength -> {
                    TextFieldValue(
                        text = oldTextField.text.replaceRange(
                            newTextField.selection.start,
                            newTextField.selection.end + 1,
                            "F"
                        ),
                        selection = newTextField.selection
                    )
                }

                else -> {
                    newTextField
                }
            }
             */
        }
    )
}

@Composable
private fun getHexCharacterRes(): Map<Char, Painter> {
    return mapOf(
        Pair('0', painterResource(Res.drawable._0)),
        Pair('1', painterResource(Res.drawable._1)),
        Pair('2', painterResource(Res.drawable._2)),
        Pair('3', painterResource(Res.drawable._3)),
        Pair('4', painterResource(Res.drawable._4)),
        Pair('5', painterResource(Res.drawable._5)),
        Pair('6', painterResource(Res.drawable._6)),
        Pair('7', painterResource(Res.drawable._7)),
        Pair('8', painterResource(Res.drawable._8)),
        Pair('9', painterResource(Res.drawable._9)),
        Pair('a', painterResource(Res.drawable.a)),
        Pair('b', painterResource(Res.drawable.b)),
        Pair('c', painterResource(Res.drawable.c)),
        Pair('d', painterResource(Res.drawable.d)),
        Pair('e', painterResource(Res.drawable.e)),
        Pair('f', painterResource(Res.drawable.f)),
        Pair('g', painterResource(Res.drawable.g)),
        Pair('h', painterResource(Res.drawable.h)),
        Pair('i', painterResource(Res.drawable.i)),
        Pair('j', painterResource(Res.drawable.j)),
        Pair('k', painterResource(Res.drawable.k)),
        Pair('l', painterResource(Res.drawable.l)),
        Pair('m', painterResource(Res.drawable.m)),
        Pair('n', painterResource(Res.drawable.n)),
        Pair('o', painterResource(Res.drawable.o)),
        Pair('p', painterResource(Res.drawable.p)),
        Pair('q', painterResource(Res.drawable.q)),
        Pair('r', painterResource(Res.drawable.r)),
        Pair('s', painterResource(Res.drawable.s)),
        Pair('t', painterResource(Res.drawable.t)),
        Pair('u', painterResource(Res.drawable.u)),
        Pair('v', painterResource(Res.drawable.v)),
        Pair('w', painterResource(Res.drawable.w)),
        Pair('x', painterResource(Res.drawable.x)),
        Pair('y', painterResource(Res.drawable.y)),
        Pair('z', painterResource(Res.drawable.z))
    )
}