package org.mr0btain.tunerstoolkit

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Tuners Toolkit") {
        App()
    }
}
