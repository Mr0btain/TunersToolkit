@file:Suppress("FunctionName")

package components.utilitytab.maplisttab.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun HeaderView(
    title: String,
    modifier: Modifier,
    textStyle: TextStyle
) {
    Box(
        modifier = modifier
    ) {
        Text(
            text = title,
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
            color = textStyle.color,
            modifier = Modifier
                .padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                .fillMaxWidth()
        )
    }
}
