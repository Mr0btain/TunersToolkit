package org.mr0btain.tunerstoolkit

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit
import kotlinx.io.files.Path
import org.mr0btain.applogger.AppLogger
import org.mr0btain.applogger.LogBook
import org.mr0btain.applogger.LogFileStore
import org.mr0btain.tunerstoolkit.platform.DirectoryResolverFactory
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_app_icon

private const val TAG = "Main"

fun main() {
    // Sets up FileKit's dialogs and folders for this app
    FileKit.init("TunersToolkit")
    startLogFiles()

    application {
        Window(onCloseRequest = ::exitApplication, title = "Tuners Toolkit", icon = painterResource(Res.drawable.tt_app_icon)) {
            App()
        }
    }
}

/** Daily log files in the app's data folder (logs/tunerstoolkit-<date>.log), two weeks of them by default */
private fun startLogFiles() {
    try {
        val dataDir = DirectoryResolverFactory().instance.getUserDataDir("TunersToolkit", false)
        val store = LogFileStore(Path(dataDir, "logs"), filePrefix = "tunerstoolkit")
        if (store.usable) LogBook.attach(store)
    } catch (e: Exception) {
        AppLogger.e("$TAG:LogFiles", "Couldn't start saving log files", e)
    }
}
