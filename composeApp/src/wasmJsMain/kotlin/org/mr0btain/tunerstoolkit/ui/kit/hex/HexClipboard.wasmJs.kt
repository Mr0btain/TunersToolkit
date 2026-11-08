@file:OptIn(ExperimentalWasmJsInterop::class)

package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.await
import kotlin.js.Promise

/*
 * The browser's own clipboard calls, past Compose's ClipEntry: they only work on https (or localhost), and reading
 * asks the person first in some browsers
 */

actual suspend fun Clipboard.writeText(text: String) {
    browserWriteText(text).await<JsAny?>()
}

actual suspend fun Clipboard.readText(): String? = browserReadText().await<JsString?>()?.toString()

private fun browserWriteText(text: String): Promise<JsAny?> = js(
    """(navigator.clipboard && navigator.clipboard.writeText) ? navigator.clipboard.writeText(text) : Promise.reject(new Error('No clipboard'))"""
)

private fun browserReadText(): Promise<JsString?> = js(
    """(navigator.clipboard && navigator.clipboard.readText) ? navigator.clipboard.readText() : Promise.resolve(null)"""
)
