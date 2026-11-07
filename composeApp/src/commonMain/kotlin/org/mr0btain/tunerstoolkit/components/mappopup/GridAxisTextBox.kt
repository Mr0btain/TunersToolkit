@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.components.mappopup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// TODO: Make this look way better
@Composable
fun GridAxisTextBox(
    value: String,
    row: Int,
    column: Int,
    textStyle: TextStyle
) {
    Text(
        text = value,
        textAlign = TextAlign.Center,
        style = textStyle,
        modifier = Modifier
            .positionInGrid(row, column)
            .wrapContentSize()
            .padding(start = 6.dp, end = 4.dp, top = 2.dp)
    )
}