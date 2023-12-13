package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class MaxRefresh(
    val scalingUnit: UInt = UInt.MIN_VALUE,
    val rate: ULong = ULong.MIN_VALUE
): A2lOptional
