package components.custom.lazycontainer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier

@Composable
fun LazyContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val mItemIndex = mutableIntStateOf(0)
    val mItemTotalCount = mutableIntStateOf(0)
    val mScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(state = mScrollState)
    ) {

    }
}