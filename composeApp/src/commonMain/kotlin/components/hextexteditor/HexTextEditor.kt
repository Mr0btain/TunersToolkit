package components.hextexteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HexTextEditor(
    input: String,
    modifier: Modifier
) {
    val charsToGet = "abcdefghijklmnopqrstuvwxyz0123456789"
    val chars = mutableMapOf<Char, Painter>()
    val textSize = Size(width = 16f, height = 38f)

    charsToGet.forEach {
        chars[it] = painterResource("svg/$it.xml")
    }

    Canvas(
        modifier = modifier
    ) {
        var lastCharSize = 0f

        input.forEachIndexed { index, char ->
            if (index == 0) {
                translate(top = 0f, left = lastCharSize) {
                    val objectToPaint = chars[char.lowercaseChar()]

                    if (objectToPaint != null) {
                        with(objectToPaint) {
                            draw(textSize)
                        }
                    }
                }
            } else {
                translate(top = 0f, left = lastCharSize) {
                    val objectToPaint = chars[char.lowercaseChar()]

                    if (objectToPaint != null) {
                        with(objectToPaint) {
                            draw(textSize)
                        }
                    }
                }
            }

            lastCharSize += textSize.width
        }
    }
}