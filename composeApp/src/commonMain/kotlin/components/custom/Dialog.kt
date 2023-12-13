package components.custom

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun AlertDialogView(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: Painter,
) {
    AlertDialog(
        modifier = modifier,
        icon = {
            Icon(icon, contentDescription = "", modifier = Modifier.alpha(1f),)
        },
        title = {
            Text(text = dialogTitle, modifier = Modifier.alpha(1f),)
        },
        text = {
            Text(text = dialogText, modifier = Modifier.alpha(1f),)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.alpha(1f),
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.alpha(1f),
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}