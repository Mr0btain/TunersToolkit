package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class BitMask(val mask: ULong = ULong.MIN_VALUE): A2lOptional
