package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.writeText(text: String) {
    setClipEntry(ClipEntry.withPlainText(text))
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.readText(): String? = getClipEntry()?.getPlainText()
