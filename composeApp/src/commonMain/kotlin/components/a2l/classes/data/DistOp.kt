package components.a2l.classes.data

import components.a2l.classes.enums.DataType
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class DistOp(
    val dataType: DataType = DataType.Default,
    val position: Int = Int.MIN_VALUE
): A2lOptional