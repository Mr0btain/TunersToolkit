package components.formulaparser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import components.formulaparser.components.ExpressionParser

@Composable
fun FormulaParseDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    val mFormulaText = remember { mutableStateOf(TextFieldValue()) }
    val result = mutableDoubleStateOf(0.0)

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Input an equation:")

                Spacer(modifier = Modifier.padding(6.dp))

                OutlinedTextField(
                    value = mFormulaText.value,
                    onValueChange = { mFormulaText.value = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { result.value = ExpressionParser().parse(mFormulaText.value.text) }
                    )
                )

                Spacer(modifier = Modifier.padding(6.dp))

                Text(text = "Result: ${result.value}")

                Spacer(modifier = Modifier.padding(6.dp))

                Row {
                    TextButton(onClick = onDismissRequest) { Text(text = "Cancel") }

                    TextButton(onClick = { result.value = ExpressionParser().parse(mFormulaText.value.text) }) { Text(text = "Calculate") }
                }
            }
        }
    }
}