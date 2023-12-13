package components.parsers.a2l.classes.enums

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class GuardRails: A2lOptional {
    True,
    False
}