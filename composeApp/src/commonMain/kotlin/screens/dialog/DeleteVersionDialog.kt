package screens.dialog

import androidx.compose.runtime.Composable
import components.parsers.a2l.classes.data.A2lData
import components.custom.AlertDialogView
import components.datastorage.ProjectFilesManager
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DeleteVersionDialog(
    onDismissRequest: () -> Unit,
    projectName: String,
    versionName: String,
    filesManager: ProjectFilesManager,
    viewModel: MainViewModel, scope: CoroutineScope
) {
    AlertDialogView(
        onDismissRequest = { onDismissRequest() },
        onConfirmation = {
            scope.launch(Dispatchers.Default) {
                viewModel.showDeleteVersionDialog.value = false
                viewModel.isLoading.value = true
                filesManager.removeProjectVersion(name = projectName, version = versionName)
                viewModel.isLoading.value = false
            }
        },
        dialogTitle = "Delete Version?",
        dialogText = "Are you sure you want to delete this version: $versionName?",
        icon = painterResource("icons/delete.png")
    )
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}