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
 * Test for opening files into the editor
 */

private const val TAG = "Open07kTestProject"

@Suppress("FunctionName")
@Composable
fun Open07kTestProject(
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

    // The 07K files are real OEM files, so they're not in git. Put them in testdata/local/07k/ to use this
    val hexFile = readLocalTestFile("07k/07K906032BR_5703.hex")
    val a2lFile = readLocalTestFile("07k/07K906032BR_5703.A2L")
    if (hexFile == null || a2lFile == null) {
        AppLogger.w("$TAG:Open", "07K test files not found in testdata/local/07k, see docs/testing.md")
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
        name = "07K Test",
        projectVersion = "Original",
        manufacturer = "Volkswagen",
        model = "Jetta",
        engine = "2.5L",
        transmission = "Manual",
        ecuBrand = "Bosch",
        ecuVersion = "07K906032BR_5703"
    )
    if (currentScreen?.route != "EDITOR") {
        navigator.navigate(route = "EDITOR",)
    }
}