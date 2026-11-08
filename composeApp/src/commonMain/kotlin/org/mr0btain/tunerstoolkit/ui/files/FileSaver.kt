package org.mr0btain.tunerstoolkit.ui.files

import androidx.compose.runtime.Composable

/** Saves bytes to a file the person picks: a save dialog, or on the web a browser download */
fun interface FileSaver {
    /**
     * @param name The file name offered, without the extension
     * @param extension Without the dot, like "hex"
     */
    fun save(bytes: ByteArray, name: String, extension: String)
}

/** @param onSaved Called with the saved file's name, or null when the person cancelled */
@Composable
expect fun rememberFileSaver(onSaved: (String?) -> Unit = {}): FileSaver
