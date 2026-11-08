package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.writeText(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.readText(): String? {
    val transferable = getClipEntry()?.asAwtTransferable ?: return null
    if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    return transferable.getTransferData(DataFlavor.stringFlavor) as? String
}
