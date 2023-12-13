package components.a2l.classes.data

import components.a2l.ifdata.IfData
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class IfDataList(val ifData: List<IfData> = listOf()): A2lOptional
