package screens.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import components.a2l.parseAndComputeA2lData
import components.a2l.parser.parseA2lFile
import components.functions.filetools.FilePicker
import components.functions.filetools.FileType
import components.hex.processHexFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import screens.editor.components.MainViewModel

@Composable
fun ImportDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: MainViewModel,
    focusRequester: FocusRequester,scope: CoroutineScope
) {
    val mShowHexFilePicker = remember { mutableStateOf(false) }
    val mShowA2lFilePicker = remember { mutableStateOf(false) }

    if (mShowHexFilePicker.value) {
        FilePicker(
            fileType = FileType.ALL,
            fileAction = {
                scope.launch(Dispatchers.IO) {
                    processHexFile(it, viewModel)
                } },
            filePickCloser = mShowHexFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    if (mShowA2lFilePicker.value) {
        FilePicker(
            fileType = FileType.A2l,
            fileAction = {
                scope.launch {
                    parseAndComputeA2lData(it, viewModel)
                } },
            filePickCloser = mShowA2lFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier
                .wrapContentWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Import:",
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    TextButton(
                        onClick = { mShowA2lFilePicker.value = true }
                    ) { Text("A2L File") }

                    TextButton(
                        onClick = { mShowHexFilePicker.value = true }
                    ) { Text("Hex File") }
                }

                Spacer(Modifier.padding(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    TextButton(
                        onClick = { onDismissRequest() }
                    ) { Text("Dismiss") }
                }
            }
        }
    }
}