@file:Suppress("FunctionName")

package components.utilitytab.maplisttab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun MapListSubItemView(
    buttonText: String,
    textStyle: TextStyle,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                start = 40.dp,
                top = 4.dp,
                end = 4.dp,
                bottom = 4.dp
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = {}
            )
            .background(textStyle.background)
            .padding(2.dp)
    ) {
        Text(
            text = buttonText,
            fontSize = textStyle.fontSize * 0.8,
            fontFamily = textStyle.fontFamily,
            color = textStyle.color,
            modifier = Modifier
                .padding(2.dp)
        )
    }
    Spacer(modifier = Modifier.padding(all = 4.dp))
}