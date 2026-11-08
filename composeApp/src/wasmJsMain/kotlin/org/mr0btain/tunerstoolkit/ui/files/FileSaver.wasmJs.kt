package org.mr0btain.tunerstoolkit.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.coroutines.launch

/** Browsers have no save dialog a page can open, so the file is downloaded */
@Composable
actual fun rememberFileSaver(onSaved: (String?) -> Unit): FileSaver {
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onSaved)
    return remember {
        FileSaver { bytes, name, extension ->
            val fileName = "$name.$extension"
            scope.launch {
                FileKit.download(bytes, fileName)
                saved(fileName)
            }
        }
    }
}
