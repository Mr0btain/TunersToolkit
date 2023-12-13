package components.mappopup

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import components.mappopup.GridTextBoxParentData
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Used in Grid text boxes to return the items Row and Column location within the grid
 * for the custom layout
 */
fun Modifier.positionInGrid(
    x: Int, y: Int
) = this.then(
    GridTextBoxParentData(
        row = x,
        column = y
    )
)

/**
 * Used to rotate a Composable to layout Vertically
 */
fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }

fun Offset.toIntSize(density: Density): IntSize {
    var x = 0
    var y = 0
    if (this.x != 0f) {
        x = ceil((this.x / density.density).toDouble()).roundToInt()
    }
    if (this.y != 0f) {
        y = ceil((this.y / density.density).toDouble()).roundToInt()
    }
    return IntSize(width = x, height = y)
}