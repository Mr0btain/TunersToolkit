package screens.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import components.parsers.a2l.classes.data.A2lData
import components.custom.AlertDialogView
import components.datastorage.ProjectFilesManager
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.delete

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DeleteProjectDialog(
    onDismissRequest: () -> Unit,
    projectName: String,
    filesManager: ProjectFilesManager,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    val onConfirm = mutableStateOf(false)

    if (onConfirm.value) {
        scope.launch {
            viewModel.showDeleteProjectDialog.value = false
            viewModel.isLoading.value = true
            filesManager.removeEntireProject(projectName)
            clearProjectData(viewModel)
            viewModel.isLoading.value = false
        }
        onConfirm.value = false
    }

    AlertDialogView(
        onDismissRequest = { onDismissRequest() },
        onConfirmation = { onConfirm.value = true },
        dialogTitle = "Delete Project?",
        dialogText = "Are you sure you want to delete this project: $projectName?",
        icon = painterResource(Res.drawable.delete)
    )
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}