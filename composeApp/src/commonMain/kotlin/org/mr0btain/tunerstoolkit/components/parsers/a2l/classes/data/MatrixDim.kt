package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.CharOpt
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
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

fun parseMatrixDim(input: String): MatrixDim {
    val result = input
        .replace("MATRIX_DIM", "")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }

    if (result.size >= 3) {
        return MatrixDim(
            xDim = result[0].toInt(),
            yDim = result[1].toInt(),
            zDim = result[2].toInt()
        )
    } else if (result.size == 2) {
        return MatrixDim(
            xDim = result[0].toInt(),
            yDim = result[1].toInt(),
            zDim = Int.MIN_VALUE
        )
    } else {
        return MatrixDim(
            xDim = result[0].toInt(),
            yDim = Int.MIN_VALUE,
            zDim = Int.MIN_VALUE
        )
    }
}
