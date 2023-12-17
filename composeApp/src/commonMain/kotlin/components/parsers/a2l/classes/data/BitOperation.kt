package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class BitOperation(
    val leftShift: ULong = ULong.MIN_VALUE,
    val rightShift: ULong = ULong.MIN_VALUE,
    val signExtended: Boolean = false
): A2lOptional

fun List<String>.parseBitOperation(): BitOperation {
    val data = mutableListOf<String>()
    var left = ULong.MIN_VALUE
    var right = ULong.MIN_VALUE
    var sign = false
    for (l in this) {
        when {
            l.startsWith("/end BIT_OPERATION") -> break
            else -> data.add(l)
        }
    }
    data.forEach {
        when {
            it.startsWith("LEFT_SHIFT") -> {
                left = it.replace("LEFT_SHIFT", "").trim().toULong()
            }
            it.startsWith("RIGHT_SHIFT") -> {
                right = it.replace("LEFT_SHIFT", "").trim().toULong()
            }
            it.startsWith("SIGN_EXTEND") -> sign = true
        }
    }
    return BitOperation(
        leftShift = left,
        rightShift = right,
        signExtended = sign
    )
}