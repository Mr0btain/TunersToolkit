package components.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import screens.editor.components.MainViewModel

@Composable
fun ExposedDropDownMenu(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    expanded: MutableState<Boolean>,
    content: @Composable ColumnScope.(String) -> Unit
) {
    var mIsEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel.hexDataSegments.size) {
        if (viewModel.hexDataSegments.size > 0) mIsEnabled = true
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        shadowElevation = 4.dp
    ) {
        TextButton(
            enabled = mIsEnabled,
            onClick = { expanded.value = !expanded.value },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .wrapContentSize()
        ) {
            Text(
                text = viewModel.selectedHexSegment.value,
                color = MaterialTheme.colorScheme.onPrimary,
                softWrap = false,
                maxLines = 1
            )

            Icon(
                imageVector = if (expanded.value) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = "Open or Close Menu",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) { content(viewModel.selectedHexSegment.value) }
    }
}

@Composable
fun ExposedDropDownMenuItem(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}