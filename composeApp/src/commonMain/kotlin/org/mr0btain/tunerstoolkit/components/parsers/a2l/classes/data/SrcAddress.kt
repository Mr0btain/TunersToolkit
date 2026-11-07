package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class SrcAddress(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default
): A2lOptional