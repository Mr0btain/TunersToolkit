package components.functions.filetools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import org.koin.core.KoinApplication.Companion.init

@Composable
actual fun FilePicker(
    fileType: FileType,
    fileAction: (ByteArray) -> Unit,
    filePickCloser: MutableState<Boolean>,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val mFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { result ->
        try {
            if (result != null) {
                val mItem = result.let { context.contentResolver.openInputStream(it) }
                if (mItem != null) { fileAction(mItem.readBytes()) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            filePickCloser.value = false
        }
    }

    when (fileType) {
        FileType.ALL -> { SideEffect { mFileLauncher.launch("*/*") } }
        FileType.TEXT -> TODO()
        FileType.HEX -> TODO()
        FileType.A2l -> { SideEffect { mFileLauncher.launch("*/*") } }
    }
}