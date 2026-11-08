package org.mr0btain.tunerstoolkit.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger

/** The platform's save dialog, then the bytes written to what was picked */
@Composable
actual fun rememberFileSaver(onSaved: (String?) -> Unit): FileSaver {
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onSaved)
    return remember {
        FileSaver { bytes, name, extension ->
            scope.launch {
                try {
                    val file = FileKit.openFileSaver(suggestedName = name, defaultExtension = extension)
                    if (file == null) {
                        saved(null)
                    } else {
                        file.write(bytes)
                        saved(file.name)
                    }
                } catch (e: Exception) {
                    AppLogger.e("FileSaver:Write", "Couldn't save $name.$extension", e)
                    saved(null)
                }
            }
        }
    }
}
