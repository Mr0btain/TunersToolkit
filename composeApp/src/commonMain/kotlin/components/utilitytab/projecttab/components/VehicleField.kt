package components.utilitytab.projecttab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.utilitytab.projecttab.components.InputTextOption
import moe.tlaster.precompose.viewmodel.viewModel
import screens.editor.components.MainViewModel

// TODO: Maybe add drop down menu for autofill?
@Composable
fun VehicleField(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    manufacturer: String,
    model: String,
    engine: String,
    transmission: String,
    viewModel: MainViewModel
) {
    val mManufacturer = remember(viewModel.projectInfo.value.manufacturer) { mutableStateOf(manufacturer) }
    val mModel = remember(viewModel.projectInfo.value.model) { mutableStateOf(model) }
    val mEngine = remember(viewModel.projectInfo.value.engine) { mutableStateOf(engine) }
    val mTransmission = remember(viewModel.projectInfo.value.transmission) { mutableStateOf(transmission) }
    val isManufacturerChanged = derivedStateOf { viewModel.projectInfo.value.manufacturer != mManufacturer.value }
    val isModelChanged = derivedStateOf { viewModel.projectInfo.value.model != mModel.value }
    val isEngineChanged = derivedStateOf { viewModel.projectInfo.value.engine != mEngine.value }
    val isTransmissionChanged = derivedStateOf { viewModel.projectInfo.value.transmission != mTransmission.value }

    if (isManufacturerChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(manufacturer = mManufacturer.value) }
    if (isModelChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(model = mModel.value) }
    if (isEngineChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(engine = mEngine.value) }
    if (isTransmissionChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(transmission = mTransmission.value) }

    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Vehicle",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = textStyle.fontSize.times(1.8),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(4.dp)
            )

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                InputTextOption(
                    label = "Manufacturer:",
                    textFieldText = mManufacturer.value,
                    textFieldOnChange = { mManufacturer.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                InputTextOption(
                    label = "Model:",
                    textFieldText = mModel.value,
                    textFieldOnChange = { mModel.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )

                InputTextOption(
                    label = "Engine:",
                    textFieldText = mEngine.value,
                    textFieldOnChange = { mEngine.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                InputTextOption(
                    label = "Transmission:",
                    textFieldText = mTransmission.value,
                    textFieldOnChange = { mTransmission.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }
        }
    }
}