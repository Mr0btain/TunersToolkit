package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.AddressType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.IndexIncrement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class RecordLayoutAxisPoint (
    val addressType: AddressType = AddressType.NOT_SET,
    val dataSize: DataSize = DataSize.NOT_SET, // Might be the same as addressType
    val dataType: DataType = DataType.Default,
    val indexIncrement: IndexIncrement = IndexIncrement.NOT_SET,
    val indexOrder: Int = Int.MIN_VALUE,
    val position: Int = Int.MIN_VALUE
): A2lOptional

@Serializable
data class RecordLayoutAxisRescale(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default,
    val maxNumberOfRescalePairs: Int = Int.MIN_VALUE,
    val indexOrder: Int = Int.MIN_VALUE,
    val indexIncrement: IndexIncrement = IndexIncrement.NOT_SET,
    val addressType: AddressType = AddressType.NOT_SET,
): A2lOptional

@Serializable
data class RecordLayoutOffset(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default
): A2lOptional

@Serializable
data class RecordLayoutReserved(
    val multiple: Boolean = true,
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default
): A2lOptional

@Serializable
data class RecordLayoutIdentification(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default
): A2lOptional