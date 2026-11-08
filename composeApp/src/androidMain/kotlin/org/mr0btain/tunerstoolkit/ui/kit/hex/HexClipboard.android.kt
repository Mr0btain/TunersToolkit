package org.mr0btain.tunerstoolkit.ui.kit.hex

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.writeText(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("bytes", text)))
}

actual suspend fun Clipboard.readText(): String? {
    val clip = getClipEntry()?.clipData ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).text?.toString()
}
