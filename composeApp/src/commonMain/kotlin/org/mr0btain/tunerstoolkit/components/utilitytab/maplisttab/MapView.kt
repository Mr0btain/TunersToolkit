@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab.classes.MapDataContainer
import org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab.components.MapListMainItemView
import org.mr0btain.tunerstoolkit.MainViewModel
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme

@Composable
fun MapsView(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scrollState: ScrollState,
    textStyle: TextStyle
) {
    val mapGroups = viewModel.mapListItems

    TunerToolkitTheme {
        Box(
            modifier = if (mapGroups.size <= 5) {
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
                if (mapGroups.isNotEmpty()) {
                    items(mapGroups) { item ->
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