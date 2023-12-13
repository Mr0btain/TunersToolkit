package components.parsers.a2l.classes.enums

import kotlinx.serialization.Serializable

/**
 * @property INTERN Internal segment
 * @property EXTERN External segment
 */
@Serializable
enum class Attribute {
    INTERN,
    EXTERN,
    NOT_SET;
}

fun String.parseAttribute(): Attribute {
    return when (this.trim()) {
        "INTERN" -> Attribute.INTERN
        "EXTERN" -> Attribute.EXTERN
        else -> Attribute.NOT_SET
    }
}