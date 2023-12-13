package components.custom.customlazycolumn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun CustomLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState,
    height: Int,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    println("ScrollState; ${scrollState.value}")

    val mTempLazyColumn = @Composable {
        Box(modifier.fillMaxSize())
    }

    val mActualColumn = @Composable {
        LazyColumn(
            modifier = modifier
                .verticalScroll(state = scrollState)
                .height(0.dp)
                .zIndex(1f),
            state = state,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Layout(contents = listOf(mTempLazyColumn, mActualColumn)) {(mTempMeasure, mActual), constraint ->
            val mTempPlaceable = mTempMeasure[0].measure(constraint)
            val mActualPlaceable = mActual[0].measure(constraints = Constraints.fixed(width = mTempPlaceable.width, height = mTempPlaceable.height))

            layout(width = 0, height = 0) {
                mActualPlaceable.place(x = 0, y = 0)
            }
        }
    }
}