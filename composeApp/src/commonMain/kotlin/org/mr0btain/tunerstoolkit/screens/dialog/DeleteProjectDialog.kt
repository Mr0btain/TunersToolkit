package org.mr0btain.tunerstoolkit.screens.dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.custom.AlertDialogView
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.tunerstoolkit.MainViewModel
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