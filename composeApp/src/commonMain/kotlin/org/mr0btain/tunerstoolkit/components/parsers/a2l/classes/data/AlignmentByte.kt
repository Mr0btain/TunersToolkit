package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class AlignmentByte(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentWord(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentLong(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentFloat16(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentFloat32(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentFloat64(val num: Int = Int.MIN_VALUE): A2lOptional

@Serializable
data class AlignmentInt(val num: Int = Int.MIN_VALUE): A2lOptional
