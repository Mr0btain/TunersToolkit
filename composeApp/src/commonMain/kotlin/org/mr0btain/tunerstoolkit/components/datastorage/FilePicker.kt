package org.mr0btain.tunerstoolkit.components.datastorage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.components.datastorage.components.FileType
import org.mr0btain.applogger.AppLogger

private const val TAG = "FilePicker"

/**
 * Opens the platform's file picker as soon as it's composed and hands the picked file's bytes to [fileAction].
 * [filePickCloser] is set to false once the picker closes, picked or not.
 *
 * Every [FileType] shows all files for now: A2L and HEX files have no registered MIME type, so an extension filter
 * would hide them on Android
 */
@Suppress("FunctionName", "UNUSED_PARAMETER")
@Composable
fun FilePicker(
    fileType: FileType,
    fileAction: (ByteArray) -> Unit,
    filePickCloser: MutableState<Boolean>,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    val launcher = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        filePickCloser.value = false
        if (file == null) return@rememberFilePickerLauncher

        scope.launch {
            try {
                fileAction(file.readBytes())
            } catch (e: Exception) {
                AppLogger.e("$TAG:Read", "Couldn't read ${file.name}", e)
            }
        }
    }

    LaunchedEffect(Unit) { launcher.launch() }
}
