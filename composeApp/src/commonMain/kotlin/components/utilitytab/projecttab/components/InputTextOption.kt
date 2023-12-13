package components.utilitytab.projecttab.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ui.theme.TunerToolkitTheme

@Composable
fun InputTextOption(
    modifier: Modifier = Modifier,
    label: String,
    textFieldText: String,
    textFieldOnChange: (String) -> Unit,
    textStyle: TextStyle
) {
    TunerToolkitTheme {
        Row(modifier = modifier.wrapContentSize().padding(4.dp)) {
            Text(text = label, style = textStyle.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(end = 8.dp))

            BasicTextField(
                value = textFieldText,
                onValueChange = textFieldOnChange,
                textStyle = textStyle
            )
        }
    }
}