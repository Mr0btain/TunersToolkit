@file:Suppress("FunctionName")

package screens.editor.components.hexview

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import components.calibrationdata.classes.MapDataValues
import components.parsers.hex.hexToAscii
import components.maptextoverlay.classes.DimensionMapper
import components.maptextoverlay.components.getMapPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import MainViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt

// TODO: Fix maps on selection color change
// TODO: Add map double clock to open popup

@Composable
fun EditorTextField(
    hexData: MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    readOnly: Boolean,
    coroutineScope: CoroutineScope,
    focusRequester: FocusRequester,
    asciiTextFieldValue: MutableState<TextFieldValue>,
    isUpdateFromOther: MutableState<Boolean>,
    mapDataValues: List<MapDataValues>,
    unitNumber: Int,
    totalUnits: Int,
    chunkId: Int,
    textStyle: TextStyle
) {
    val digitsPerLine = viewModel.digitsPerLine
    // val mBitSize = viewModel.bitsPer.collectAsState()
    val interaction = remember { MutableInteractionSource() }
    val maxLines = hexData.value.text.length / digitsPerLine.value
    val textLayoutResultMutableState = remember { mutableStateOf<TextLayoutResult?>(null) }
    val mapper by remember { mutableStateOf(DimensionMapper()) }
    val mapPaths = remember { mutableStateListOf<Path>() }
    val selectedMap by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = textLayoutResultMutableState.value?.size) {
        textLayoutResultMutableState.value?.let { textLayout ->
            mapper.calculateValues(textLayout)
        }

        mapPaths.clear()

        mapDataValues.forEach {
            mapPaths.add(it.getMapPath(dimensMap = mapper, textStart = chunkId))
        }
    }

    BoxWithConstraints(
        modifier = when {
            (unitNumber == 0) -> {
                modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .focusRequester(focusRequester)
            }

            unitNumber == totalUnits -> {
                modifier
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .focusRequester(focusRequester)
            }

            else -> modifier
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .focusRequester(focusRequester)
        }
    ) {
        BasicTextField(
            hexData.value,
            onValueChange = { result ->
                hexData.value = EditorOnValueChanged(result, hexData.value)
                //mSelectedTextPosition = result.selection.start

                // If it's not from another view, update the other views
                if (!isUpdateFromOther.value) {
                    coroutineScope.launch {
                        isUpdateFromOther.value = true

                        // Update ascii text view with the changes
                        asciiTextFieldValue.value =
                            asciiTextFieldValue.value.copy(text = hexData.value.text.hexToAscii())

                        isUpdateFromOther.value = false
                    }
                }
            },
            modifier = Modifier
                .align(
                    Alignment.Center
                )
                .focusRequester(focusRequester)
                .padding(start = 4.dp, end = 4.dp)
                .drawBehind {
                    mapPaths.forEachIndexed { index, path ->
                        drawPath(
                            path = path,
                            color = viewModel.mapFillColor.value,
                            alpha = 0.2f
                        )

                        drawPath(
                            path = path,
                            color = if (selectedMap == index + 1) {
                                viewModel.mapSelectedOutlineColor.value
                            } else viewModel.mapOutlineColor.value,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.cornerPathEffect(2.dp.toPx()),
                            )
                        )
                    }
                },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            textStyle = textStyle.copy(textAlign = TextAlign.Center),
            maxLines = if (maxLines > 0) maxLines else 1,
            visualTransformation = EditorVisualTransformation(digitsPerLine.value),
            onTextLayout = { textLayoutResultMutableState.value = it },
            readOnly = readOnly,
            interactionSource = interaction
        )
    }
}
