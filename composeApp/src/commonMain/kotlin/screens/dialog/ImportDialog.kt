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
import components.parsers.a2l.parseAndComputeA2lData
import components.datastorage.FilePicker
import components.datastorage.components.FileType
import components.parsers.hex.processHexFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import MainViewModel

@Composable
fun ImportDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: MainViewModel,
    focusRequester: FocusRequester, scope: CoroutineScope
) {
    val showHexFilePicker = remember { mutableStateOf(false) }
    val showA2lFilePicker = remember { mutableStateOf(false) }

    if (showHexFilePicker.value) {
        FilePicker(
            fileType = FileType.ALL,
            fileAction = {
                onDismissRequest()

                scope.launch(Dispatchers.IO) {
                    processHexFile(it, viewModel)
                } },
            filePickCloser = showHexFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    if (showA2lFilePicker.value) {
        FilePicker(
            fileType = FileType.A2l,
            fileAction = {
                onDismissRequest()

                scope.launch(Dispatchers.Default) {
                    parseAndComputeA2lData(it, viewModel)
                } },
            filePickCloser = showA2lFilePicker,
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
                        onClick = { showA2lFilePicker.value = true }
                    ) { Text("A2L File") }

                    TextButton(
                        onClick = { showHexFilePicker.value = true }
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