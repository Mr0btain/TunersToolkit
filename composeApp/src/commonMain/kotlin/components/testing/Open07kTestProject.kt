package components.testing

import MainViewModel
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import components.maptextoverlay.classes.MapValuesSorted
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.parseAndComputeA2lData
import components.parsers.hex.processHexFile
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import tunerstoolkit.composeapp.generated.resources.Res

/**
 * Test for opening files into the editor
 */

@Suppress("FunctionName")
@OptIn(ExperimentalResourceApi::class)
@Composable
fun Open07kTestProject(
    navigator: NavHostController,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    // Get our current screen
    println("Get current screen")
    val currentScreen = navigator.currentDestination

    // Clear all the project data currently set
    println("Clear current project data")
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()

    // Open our test hex file
    scope.launch(Dispatchers.IO) {
        println("Opening Hex File")
        processHexFile(Res.readBytes("files/07k/07K906032BR_5703.hex"), viewModel)
    }

    // Open out test A2L File
    scope.launch {
        println("Opening A2L File")
        parseAndComputeA2lData(Res.readBytes("files/07l/07K906032BR_5703.A2L"), viewModel)
    }

    println("Setting project info")
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

    println("Navigate to Editor")
    if (currentScreen?.route != "EDITOR") {
        navigator.navigate(route = "EDITOR",)
    }
}