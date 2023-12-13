package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class MaxGradient(val value: Double = Double.NaN): A2lOptional
