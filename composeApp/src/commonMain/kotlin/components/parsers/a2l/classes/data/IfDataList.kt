package components.parsers.a2l.classes.data

import components.parsers.a2l.ifdata.IfData
import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class IfDataList(val ifData: List<components.parsers.a2l.ifdata.IfData> = listOf()): A2lOptional
