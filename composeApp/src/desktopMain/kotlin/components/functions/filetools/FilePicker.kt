package components.functions.filetools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun FilePicker(
    fileType: FileType,
    fileAction: (ByteArray) -> Unit,
    filePickCloser: MutableState<Boolean>,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    var result: File?

    // fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    when (fileType) {
        FileType.ALL -> {
            JFileChooser().apply {
                showOpenDialog(null)
                result = selectedFile
            }
        }
        FileType.TEXT -> {
            JFileChooser().apply {
                this.isAcceptAllFileFilterUsed = false
                this.addChoosableFileFilter(FileNameExtensionFilter("Text File", "txt"))

                showOpenDialog(null)
                result = selectedFile
            }
        }
        FileType.HEX -> {
            JFileChooser().apply {
                this.isAcceptAllFileFilterUsed = false
                this.addChoosableFileFilter(FileNameExtensionFilter("Hex File", "hex"))

                showOpenDialog(null)
                result = selectedFile
            }
        }
        FileType.A2l -> {
            JFileChooser().apply {
                this.isAcceptAllFileFilterUsed = false
                this.addChoosableFileFilter(FileNameExtensionFilter("A2l File", "a2l"))
                this.addChoosableFileFilter(FileNameExtensionFilter("A2L File", "A2L"))

                showOpenDialog(null)
                result = selectedFile
            }
        }
    }

    scope.launch(Dispatchers.IO) {
        result?.readBytes()?.let { fileAction(it) }
    }

    filePickCloser.value = false
}