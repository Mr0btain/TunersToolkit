package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class FixNoAxisPts(
    val numberOfAxisPts: Int = Int.MIN_VALUE
): A2lOptional