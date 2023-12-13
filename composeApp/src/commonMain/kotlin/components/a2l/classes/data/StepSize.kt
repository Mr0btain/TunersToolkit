package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class StepSize(val size: Float = Float.NaN): A2lOptional
