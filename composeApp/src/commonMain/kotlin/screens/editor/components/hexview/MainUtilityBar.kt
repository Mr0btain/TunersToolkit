@file:Suppress("FunctionName")

package screens.editor.components.hexview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import components.custom.ExposedDropDownMenu
import components.custom.ExposedDropDownMenuItem
import components.utilitytab.buttonbartab.components.UtilityIconButton
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel
import tunerstoolkit.composeapp.generated.resources.*
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.add
import tunerstoolkit.composeapp.generated.resources.copy
import tunerstoolkit.composeapp.generated.resources.remove_done

// The main Utility bar that is always shown
@OptIn(ExperimentalResourceApi::class)
@Composable
fun MainUtilityBar(
    addOnClick: (String) -> Unit,
    subOnClick: (String) -> Unit = {},
    percentOnClick: (String) -> Unit = {},
    mapOnClick: () -> Unit = {},
    viewModel: MainViewModel,
    scope: CoroutineScope,
    expanded: MutableState<Boolean>
) {
    val isDropDownExpanded = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = painterResource(Res.drawable.add)
        ) { addOnClick("Add") }

        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = painterResource(Res.drawable.remove_done)
        ) { subOnClick("Subtract") }

        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = painterResource(Res.drawable.copy)
        ) { percentOnClick("Percentage") }

        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = painterResource(Res.drawable.search)
        ) {}

        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = painterResource(Res.drawable.cut)
        ) {}

        UtilityIconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            icon = if (expanded.value) painterResource(Res.drawable.compress) else painterResource(Res.drawable.expand)
        ) { expanded.value = !expanded.value }

        ExposedDropDownMenu(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer),
            viewModel = viewModel,
            expanded = isDropDownExpanded
        ) {
            viewModel.hexDataSegments.forEach { segment ->
                if (segment.key != it) {
                    ExposedDropDownMenuItem(
                        label = segment.key,
                        onClick = {
                            isDropDownExpanded.value = false
                            viewModel.selectedHexSegment.value = segment.key
                        }
                    )
                }
            }
        }
    }
}