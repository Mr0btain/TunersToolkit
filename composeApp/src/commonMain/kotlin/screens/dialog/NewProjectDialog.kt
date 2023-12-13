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
import components.a2l.classes.data.A2lData
import components.a2l.parseAndComputeA2lData
import components.datastorage.ProjectFilesManager
import components.functions.filetools.FilePicker
import components.functions.filetools.FileType
import components.functions.filetools.saveNewProject
import components.hex.processHexFile
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import screens.editor.components.MainViewModel

@Composable
fun NewProjectDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: MainViewModel,
    filesManager: ProjectFilesManager,
    focusRequester: FocusRequester,
    scope: CoroutineScope
) {
    val mProjectName = remember { mutableStateOf("") }
    val mVersionName = remember { mutableStateOf("") }
    val mEcuVersion = remember { mutableStateOf("") }
    val mEcuBrand = remember { mutableStateOf("") }
    val mVehicleManufacturer = remember { mutableStateOf("") }
    val mVehicleModel = remember { mutableStateOf("") }
    val mEngine = remember { mutableStateOf("") }
    val mTransmission = remember { mutableStateOf("") }
    val mShowHexFilePicker = remember { mutableStateOf(false) }
    val mShowA2lFilePicker = remember { mutableStateOf(false) }

    if (mShowHexFilePicker.value) {
        FilePicker(
            fileType = FileType.ALL,
            fileAction = {
                scope.launch(Dispatchers.IO) {
                    processHexFile(it, viewModel)
                }
            },
            filePickCloser = mShowHexFilePicker,
            scope = scope,
            focusRequester = focusRequester
        )
    }

    if (mShowA2lFilePicker.value) {
        FilePicker(
            fileType = FileType.A2l,
            fileAction = {
                scope.launch {
                    parseAndComputeA2lData(it, viewModel)
                }
                         },
            filePickCloser = mShowA2lFilePicker,
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
                        onClick = { mShowA2lFilePicker.value = true }
                    ) { Text("A2L File") }

                    TextButton(
                        onClick = { mShowHexFilePicker.value = true }
                    ) { Text("Hex File") }
                }

                Spacer(modifier = Modifier.padding(4.dp))

                Text(
                    text = "Name and Version:",
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                OutlinedTextField(
                    value = mProjectName.value,
                    onValueChange = { mProjectName.value = it },
                    label = { Text(text = "Project Name") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mVersionName.value,
                    onValueChange = { mVersionName.value = it },
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
                    value = mVehicleManufacturer.value,
                    onValueChange = { mVehicleManufacturer.value = it },
                    label = { Text(text = "Vehicle Manufacturer") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mVehicleModel.value,
                    onValueChange = { mVehicleModel.value = it },
                    label = { Text(text = "Vehicle Model") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mEngine.value,
                    onValueChange = { mEngine.value = it },
                    label = { Text(text = "Engine") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mTransmission.value,
                    onValueChange = { mTransmission.value = it },
                    label = { Text(text = "Transmission") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mEcuBrand.value,
                    onValueChange = { mEcuBrand.value = it },
                    label = { Text(text = "Ecu Brand") }
                )

                Spacer(modifier = Modifier.padding(4.dp))

                OutlinedTextField(
                    value = mEcuVersion.value,
                    onValueChange = { mEcuVersion.value = it },
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
                                name = mProjectName.value,
                                projectVersion = mVersionName.value.ifBlank { "Original" },
                                manufacturer = mVehicleManufacturer.value,
                                model = mVehicleModel.value,
                                engine = mEngine.value,
                                transmission = mTransmission.value,
                                ecuBrand = mEcuBrand.value,
                                ecuVersion = mEcuVersion.value
                            )

                            saveNewProject(
                                viewModel = viewModel,
                                filesManager = filesManager,
                                scope = scope,
                                projectName = mProjectName.value,
                                projectVersion = mVersionName.value.ifBlank { "Original" }
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