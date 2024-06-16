@file:Suppress("FunctionName")

package components.hextexteditor.components

import MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.forEachChange
import androidx.compose.foundation.text2.input.insert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.calibrationdata.classes.MapDataValues
import components.maptextoverlay.classes.DimensionMapper
import components.maptextoverlay.components.getMapPath

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexTextEditor(
    viewModel: MainViewModel,
    textStyle: TextStyle,
    scrollState: ScrollState,
    mapDataValues: List<MapDataValues> = listOf(),
) {
    val allowedCharacters = setOf('A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f')
    val layoutResultMutableState = remember { mutableStateOf<TextLayoutResult?>(null) }
    val dimensionMapper by remember { mutableStateOf(DimensionMapper()) }
    val mapDataPaths = remember { mutableStateListOf<Path>() }
    val currentSelectedMap by remember { mutableIntStateOf(0) }

    // Update the Paths to draw for each map when the layout changes
    LaunchedEffect(key1 = layoutResultMutableState.value?.size) {
        // Check if we even have any maps to work with
        if (mapDataValues.isNotEmpty()) {
            // Calculate the dimensions needed to calculate the Paths for each map
            layoutResultMutableState.value?.let { textLayout ->
                dimensionMapper.calculateValues(textLayout)
            }

            mapDataPaths.clear()

            mapDataValues.forEach {
                mapDataPaths.add(it.getMapPath(dimensMap = dimensionMapper, textStart = 0))
            }
        }
    }

    BasicTextField2(
        modifier = Modifier.drawBehind {
            mapDataPaths.forEachIndexed { index, path ->
                drawPath(
                    path = path,
                    color = viewModel.mapFillColor.value,
                    alpha = 0.2f
                )

                drawPath(
                    path = path,
                    color = if (currentSelectedMap == index + 1) {
                        viewModel.mapSelectedOutlineColor.value
                    } else viewModel.mapOutlineColor.value,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.cornerPathEffect(2.dp.toPx()),
                    )
                )
            }
        },
        state = viewModel.hexEditorData,
        textStyle = textStyle.copy(textAlign = TextAlign.Center),
        inputTransformation = { originalValue, valueWithChanges ->
            valueWithChanges.changes.forEachChange { new, _ ->
                when {
                    // If we've hit backspace, replace character with F
                    (new.start == new.end) -> {
                        // Check length to make sure were not at 0, 0
                        if (originalValue.length > valueWithChanges.length) {
                            val digitDeleted = originalValue[new.start]

                            // Check if the digit deleted is a space or a newline character as we don't want these deleted
                            if (!digitDeleted.isLetterOrDigit()) {
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.placeCursorBeforeCharAt(new.start)
                            } else {
                                // Insert 'F' and place cursor before it
                                valueWithChanges.insert(new.end, "F")
                                valueWithChanges.placeCursorBeforeCharAt(new.start)

                                // Handle Ascii Update
                                if (new.end > 0) {
                                    if (valueWithChanges.charAt(new.end - 1) == ' ' || valueWithChanges.charAt(new.end - 1) == '\n') {
                                        viewModel.updateAsciiEditorText(
                                            text = "F" + valueWithChanges.charAt(new.end + 1).toString(),
                                            range = IntRange(new.end, new.end + 2),
                                            hexRange = true
                                        )
                                    } else {
                                        viewModel.updateAsciiEditorText(
                                            text = valueWithChanges.charAt(new.end - 1).toString() + "F",
                                            range = IntRange(new.end, new.end + 2),
                                            hexRange = true
                                        )
                                    }
                                }
                            }
                        } else {
                            // Revert all changes
                            valueWithChanges.revertAllChanges()
                        }
                    }

                    (new.start < new.end && new.start != originalValue.length) -> {
                        // Get the input and char at the old location
                        val inputChar = valueWithChanges.charAt(new.start)
                        val oldCharInLocation = valueWithChanges.charAt(new.end)

                        if (oldCharInLocation.isLetterOrDigit()) {
                            // Check if it's an allowed character
                            if (inputChar.isDigit() || inputChar in allowedCharacters)  {
                                // Revert changes, replace the character with the new one and move cursor after it
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start, new.end, inputChar.toString().uppercase())
                                valueWithChanges.placeCursorAfterCharAt(new.start)

                                // Handle Ascii Update
                                if (new.start == 0 || valueWithChanges.charAt(new.start - 1) == ' ' || valueWithChanges.charAt(new.start) == '\n') {
                                    viewModel.updateAsciiEditorText(
                                        text = valueWithChanges.charAt(new.start).toString() + valueWithChanges.charAt(new.end),
                                        range = IntRange(new.start, new.end + 1),
                                        hexRange = true
                                    )
                                } else {
                                    viewModel.updateAsciiEditorText(
                                        text = valueWithChanges.charAt(new.start - 1).toString() + valueWithChanges.charAt(new.start),
                                        range = IntRange(new.start, new.end + 1),
                                        hexRange = true
                                    )
                                }
                            } else {
                                // Revert all changes
                                valueWithChanges.revertAllChanges()
                            }
                        } else {
                            if (inputChar.isDigit() || inputChar in allowedCharacters)  {
                                // Revert changes, replace the character with the new one and move cursor after it
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start + 1, new.end + 1, inputChar.toString().uppercase())
                                valueWithChanges.placeCursorAfterCharAt(new.start + 1)

                                viewModel.updateAsciiEditorText(
                                    text = valueWithChanges.charAt(new.start + 1).toString() + valueWithChanges.charAt(new.end + 1),
                                    range = IntRange(new.start + 1, new.end + 1),
                                    hexRange = true
                                )
                            } else {
                                // Revert all changes
                                valueWithChanges.revertAllChanges()
                            }
                        }
                    }

                    else -> { valueWithChanges.revertAllChanges() }
                }
            }
        },
        scrollState = scrollState,
        onTextLayout = { layoutResultMutableState.value = it.invoke() },
        cursorBrush = textStyle.brush ?: Brush.verticalGradient(colors = listOf(textStyle.color, textStyle.color))
    )
}