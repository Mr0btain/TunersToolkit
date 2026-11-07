package org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.components
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
import org.mr0btain.tunerstoolkit.MainViewModel
// TODO: Maybe make drop down?

@Composable
fun EcuField(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    ecuBrand: String,
    ecuModel: String,
    ecuVersion: String,
    viewModel: MainViewModel
) {
    val brand = remember(viewModel.projectInfo.value.ecuBrand) { mutableStateOf(ecuBrand) }
    val model = remember(viewModel.projectInfo.value.ecuModel) { mutableStateOf(ecuModel) }
    val version = remember(viewModel.projectInfo.value.ecuVersion) { mutableStateOf(ecuVersion) }
    val isBrandMatch = derivedStateOf { viewModel.projectInfo.value.ecuBrand != brand.value }
    val isModelMatch = derivedStateOf { viewModel.projectInfo.value.ecuModel != model.value }
    val isVersionMatch = derivedStateOf { viewModel.projectInfo.value.ecuVersion != version.value }

    if (isBrandMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuBrand = brand.value)
    }

    if (isModelMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuModel = model.value)
    }

    if (isVersionMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuVersion = version.value)
    }

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
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
                text = "Ecu",
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
                    label = "Brand:",
                    textFieldText = brand.value,
                    textFieldOnChange = { brand.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )

                InputTextOption(
                    label = "Model:",
                    textFieldText = model.value,
                    textFieldOnChange = { model.value = it },
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
                    label = "Version:",
                    textFieldText = version.value,
                    textFieldOnChange = { version.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }
        }
    }
}