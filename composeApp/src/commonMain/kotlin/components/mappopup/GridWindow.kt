package components.mappopup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.calibrationdata.classes.MapDataValues
import getPlatform
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.resize_drag
import kotlin.math.roundToInt

// TODO: Add ability to do comparisons between maps and original values
// TODO: Add actual editing logic

@OptIn(ExperimentalResourceApi::class)
@Composable
fun GridWindow(
    modifier: Modifier = Modifier,
    mapDataValues: MapDataValues,
    fontFamily: FontFamily = FontFamily.Monospace,
    axisTextSize: TextUnit,
    dataTextSize: TextUnit,
    isVisible: MutableState<List<String>>,
    onClickFocus: () -> Unit
) {
    // Test Map Popup for windows
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    var width by remember { mutableIntStateOf(400) }
    var height by remember { mutableIntStateOf(200) }
    val density = LocalDensity.current

    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible.value.contains(mapDataValues.name,
        )) {
        Box(
            modifier = modifier
                .wrapContentSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .clickable(
                    indication = null,
                    interactionSource = MutableInteractionSource()
                ) { onClickFocus() }
        ) {
            Column(
                modifier = Modifier
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .width(width.dp)
                    .height(height.dp)
            ) {
                Row(
                    modifier = Modifier
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .width(width.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .wrapContentHeight()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mapDataValues.name,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = if (getPlatform().name == "Java") 14.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = modifier
                            .wrapContentSize()
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp)
                    )

                    GridWindowButtonBar(
                        closeOnClick = {
                            // remove its name from the list of visible maps
                            val newList = isVisible.value.toMutableList()
                            newList.remove(mapDataValues.name)
                            isVisible.value = newList
                        },
                        modifier =  Modifier
                    )
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(state = scrollState, enabled = true)
                        .wrapContentSize()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 6.dp)
                ) {
                    MapGrid(
                        mapData = mapDataValues,
                        axisTextStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = axisTextSize,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        dataTextStyle = TextStyle(
                            fontSize = dataTextSize,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        ),
                        headerTextStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }


            }
            Icon(
                painterResource(Res.drawable.resize_drag),
                "Drag to Resize",
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .alpha(0.8f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val size = with(density) { dragAmount.toIntSize(this) }
                            width += size.width
                            height += size.height
                        }
                    }
            )
        }
    }
}