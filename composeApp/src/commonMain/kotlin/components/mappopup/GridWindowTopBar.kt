package components.mappopup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import getPlatform

@Composable
fun GridWindowTopBar(
    modifier: Modifier = Modifier,
    label: String,
    closeOnClick: () -> Unit
) {
    var size = IntSize(0, 0)

    Row(
        modifier = modifier
            .shadow(elevation = 2.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .fillMaxWidth()
            .wrapContentHeight()
            .onGloballyPositioned { size = it.size }
    ) {
        val label1 = @Composable {
            Text(
                text = label,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = if (getPlatform().name == "Java") 12.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }

        val buttonBar = @Composable {
            GridWindowButtonBar(closeOnClick)
        }

        Layout(
            contents = listOf(label1, buttonBar)
        ) { (mLabelMeasureable, mButtonBarMeasureable), constraints ->
            val labelPlaceable = mLabelMeasureable.first().measure(constraints)
            val buttonBarPlaceable = mButtonBarMeasureable.first().measure(constraints)

            layout(width = size.width, height = size.height) {
                labelPlaceable.place(
                    x = (size.width / 2) - (labelPlaceable.width / 2),
                    y = (size.height / 2) - (labelPlaceable.height / 2)
                )

                buttonBarPlaceable.place(
                    x = size.width - buttonBarPlaceable.width,
                    y = (size.height / 2) - (labelPlaceable.height / 2)
                )
            }
        }
    }
}