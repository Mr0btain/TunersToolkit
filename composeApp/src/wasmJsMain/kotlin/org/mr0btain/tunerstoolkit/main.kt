package org.mr0btain.tunerstoolkit

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }

    // index.html's loading message, the app draws over the page from here
    document.getElementById("loading")?.remove()
}
