@file:Suppress("FunctionName")

package components.utilitytab.maplisttab

import androidx.compose.animation.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import components.utilitytab.maplisttab.classes.MapDataContainer
import components.utilitytab.maplisttab.components.MapListMainItemView
import screens.editor.components.MainViewModel
import ui.theme.TunerToolkitTheme

@Composable
fun MapsView(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scrollState: ScrollState,
    textStyle: TextStyle
) {
    val mMapGroups = viewModel.mapListItems

    TunerToolkitTheme {
        Box(
            modifier = if (mMapGroups.size <= 5) {
                modifier
                    .fillMaxWidth()
                    .padding(all = 2.dp)
                    .background(Color.Transparent)
                    .clip(RoundedCornerShape(8.dp))
            } else {
                modifier
                    .fillMaxWidth()
                    .padding(all = 2.dp)
                    .background(Color.Transparent)
                    .clip(RoundedCornerShape(8.dp))
            }
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Loop through each item and make a container for it
                if (mMapGroups.isNotEmpty()) {
                    items(mMapGroups) { item ->
                        MapListMainItemView(
                            container = item,
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(all = 2.dp)
                                .background(color = Color.Transparent)
                                .clip(RoundedCornerShape(8.dp)),
                            textStyle = textStyle
                        )
                    }
                } else {
                    item {
                        MapListMainItemView(
                            container = MapDataContainer(title = "Maps show up here", children = listOf()),
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(all = 2.dp)
                                .background(color = Color.Transparent)
                                .clip(RoundedCornerShape(8.dp)),
                            textStyle = textStyle
                        )
                    }
                }
            }
        }
    }
}

