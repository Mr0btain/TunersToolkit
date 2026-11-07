package org.mr0btain.tunerstoolkit.components.testing
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.parseAndComputeA2lData
import org.mr0btain.tunerstoolkit.components.parsers.hex.processHexFile
import org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger

/**
 * Opens the TT-DEMO-1 demo project (testdata/demo, made by core's DemoEcu) in the editor, for trying the app
 * without a real ECU file. Desktop only for now: it reads the files from the checkout
 */

private const val TAG = "OpenDemoProject"

@Suppress("FunctionName")
@Composable
fun OpenDemoProject(
    navigator: NavHostController,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    // Get our current screen
    val currentScreen = navigator.currentDestination

    // Clear all the project data currently set
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()

    val hexFile = readTestDataFile("demo/tt-demo-1.hex")
    val a2lFile = readTestDataFile("demo/tt-demo-1.a2l")
    if (hexFile == null || a2lFile == null) {
        AppLogger.w("$TAG:Open", "The demo files aren't in testdata/demo, or this platform can't read them")
        return
    }

    scope.launch(Dispatchers.Default) {
        AppLogger.d(TAG, "Opening hex file")
        processHexFile(hexFile, viewModel)
    }

    scope.launch {
        AppLogger.d(TAG, "Opening A2L file")
        parseAndComputeA2lData(a2lFile, viewModel)
    }
    viewModel.projectInfo.value = ProjectInfo(
        name = "TT-DEMO-1",
        projectVersion = "Original",
        manufacturer = "Demo",
        model = "Test car",
        engine = "2.0L turbo",
        transmission = "Manual",
        ecuBrand = "Tuners Toolkit",
        ecuVersion = "TTDEMO1 0.1"
    )
    if (currentScreen?.route != "EDITOR") {
        navigator.navigate(route = "EDITOR",)
    }
}