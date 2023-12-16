package screens.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.parseAndComputeA2lData
import components.datastorage.ProjectFilesManager
import components.datastorage.FilePicker
import components.datastorage.components.FileType
import components.datastorage.components.saveNewProject
import components.parsers.hex.processHexFile
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import MainViewModel

@Composable
fun NewProjectDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: MainViewModel,
    filesManager: ProjectFilesManager,
    focusRequester: FocusRequester,
    scope: CoroutineScope
) {
    val projectName = remember { mutableStateOf("") }
    val versionName = remember { mutableStateOf("") }
    val ecuVersion = remember { mutableStateOf("") }
    val ecuBrand = remember { mutableStateOf("") }
    val vehicleManufacturer = remember { mutableStateOf("") }
    val vehicleModel = remember { mutableStateOf("") }
    val engine = remember { mutableStateOf("") }
    val transmission = remember { mutableStateOf("") }
    val showHexFilePicker = remember { mutableStateOf(false) }
    val showA2lFilePicker = remember { mutableStateOf(false) }

    if (showHexFilePicker.value) {
        FilePicker(
            fileType = FileType.ALL,
            fileAction = {
                scope.launch(Dispatchers.IO) {
                    processHexFile(it, viewModel)
                }
            },
            filePickCloser = showHexFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    if (showA2lFilePicker.value) {
        FilePicker(
            fileType = FileType.A2l,
            fileAction = {
                scope.launch {
                    parseAndComputeA2lData(it, viewModel)
                }
                         },
            filePickCloser = showA2lFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier
                .wrapContentWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(state = rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Import:",
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    TextButton(
                        onClick = { showA2lFilePicker.value = true }
                    ) { Text("A2L File") }

                    TextButton(
                        onClick = { showHexFilePicker.value = true }
                    ) { Text("Hex File") }
                }

                Spacer(modifier = Modifier.padding(4.dp))

                Text(
                    text = "Name and Version:",
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                OutlinedTextField(
                    value = projectName.value,
                    onValueChange = { projectName.value = it },
                    label = { Text(text = "Project Name") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = versionName.value,
                    onValueChange = { versionName.value = it },
                    label = { Text(text = "Version Name") }
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Text(
                    text = "Vehicle Details:",
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = vehicleManufacturer.value,
                    onValueChange = { vehicleManufacturer.value = it },
                    label = { Text(text = "Vehicle Manufacturer") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = vehicleModel.value,
                    onValueChange = { vehicleModel.value = it },
                    label = { Text(text = "Vehicle Model") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = engine.value,
                    onValueChange = { engine.value = it },
                    label = { Text(text = "Engine") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = transmission.value,
                    onValueChange = { transmission.value = it },
                    label = { Text(text = "Transmission") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = ecuBrand.value,
                    onValueChange = { ecuBrand.value = it },
                    label = { Text(text = "Ecu Brand") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = ecuVersion.value,
                    onValueChange = { ecuVersion.value = it },
                    label = { Text(text = "Ecu version") }
                )

                Spacer(Modifier.padding(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    TextButton(
                        onClick = { onDismissRequest() }
                    ) { Text("Dismiss") }

                    TextButton(
                        onClick = {
                            viewModel.showNewProjectDialog.value = false

                            viewModel.projectInfo.value = ProjectInfo(
                                name = projectName.value,
                                projectVersion = versionName.value.ifBlank { "Original" },
                                manufacturer = vehicleManufacturer.value,
                                model = vehicleModel.value,
                                engine = engine.value,
                                transmission = transmission.value,
                                ecuBrand = ecuBrand.value,
                                ecuVersion = ecuVersion.value
                            )

                            saveNewProject(
                                viewModel = viewModel,
                                filesManager = filesManager,
                                scope = scope,
                                projectName = projectName.value,
                                projectVersion = versionName.value.ifBlank { "Original" }
                            )
                        }
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}