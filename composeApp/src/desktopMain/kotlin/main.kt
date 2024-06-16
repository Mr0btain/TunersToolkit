import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.component.getScopeId
import org.koin.core.component.getScopeName
import java.awt.FileDialog

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Tuners Toolkit") {
        App()
    }
}