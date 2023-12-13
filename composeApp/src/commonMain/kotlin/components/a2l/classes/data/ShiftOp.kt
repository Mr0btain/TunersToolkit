package components.a2l.classes.data

import components.a2l.classes.enums.DataType
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class ShiftOp(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default
): A2lOptional