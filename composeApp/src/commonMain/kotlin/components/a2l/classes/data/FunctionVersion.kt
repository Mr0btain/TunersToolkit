package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class FunctionVersion(val version: String = ""): A2lOptional
