package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class RefCharacs(val refs: List<String> = listOf()): A2lOptional