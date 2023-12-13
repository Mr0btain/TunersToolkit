package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class MatrixDim(
    val xDim: Int = Int.MIN_VALUE,
    val yDim: Int = Int.MIN_VALUE,
    val zDim: Int = Int.MIN_VALUE
): A2lOptional {
    fun asArray(): Array<Int> {
        return if (xDim != Int.MIN_VALUE && yDim != Int.MIN_VALUE && zDim != Int.MIN_VALUE) {
            arrayOf(xDim, yDim, zDim)
        } else if (xDim != Int.MIN_VALUE && yDim != Int.MIN_VALUE) {
            arrayOf(xDim, yDim)
        } else arrayOf(xDim)
    }
}
