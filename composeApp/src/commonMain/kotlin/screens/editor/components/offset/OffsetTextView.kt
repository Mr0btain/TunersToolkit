package screens.editor.components.offset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


@Composable
fun OffsetTextView(
    modifier: Modifier = Modifier,
    value: String,
    textStyle: TextStyle,
    unitNumber: Int,
    totalUnits: Int
) {
    BoxWithConstraints(
        modifier = when {
            (unitNumber == 0) -> {
                modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(start = 4.dp, end = 4.dp)
            }

            unitNumber == totalUnits -> {
                modifier
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(start = 4.dp, end = 4.dp)
            }

            else -> modifier
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(start = 4.dp, end = 4.dp)
        }
    ) {
        BasicText(
            value,
            style = textStyle,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.Center),
        )
    }
}