package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class IfDataList(val ifData: List<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData> = listOf()): A2lOptional
