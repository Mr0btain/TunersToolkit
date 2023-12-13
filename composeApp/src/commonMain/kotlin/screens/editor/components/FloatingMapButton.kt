package screens.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import components.calibrationdata.classes.MapDataValues

@Composable
fun FloatingMapButton(
    selectedMaps: List<MapDataValues>,
    modifier: Modifier = Modifier,
    onClick: (MapDataValues) -> Unit
) {
    var mIsVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .wrapContentSize()
            .zIndex(1f)
    ) {
        AnimatedVisibility(visible = mIsVisible) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                selectedMaps.forEach {  mapValue ->
                    Button(onClick = {
                        onClick(mapValue)
                        mIsVisible = false
                    }) {
                        Text(text = mapValue.name)
                    }
                }
            }
        }

        Button(
            onClick = { mIsVisible = !mIsVisible },
            modifier = Modifier.wrapContentSize()
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Selected Maps")
            Text(text = "Maps", modifier = Modifier.padding(start = 2.dp))
        }
    }
}