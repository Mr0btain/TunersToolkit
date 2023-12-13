package components.parsers.a2l.classes.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ConversionType {
    IDENTICAL,
    LINEAR,
    RAT_FUNC,
    TAB_INTP,
    TAB_NOINTP,
    TAB_VERB,
    FORM,
    NOT_SET;
}

fun String.parseConversionType(): ConversionType {
    return when (this.trim()) {
        "IDENTICAL" -> ConversionType.IDENTICAL
        "FORM" -> ConversionType.FORM
        "LINEAR" -> ConversionType.LINEAR
        "RAT_FUNC" -> ConversionType.RAT_FUNC
        "TAB_INTP" -> ConversionType.TAB_INTP
        "TAB_NOINTP" -> ConversionType.TAB_NOINTP
        "TAB_VERB" -> ConversionType.TAB_VERB
        else -> ConversionType.NOT_SET
    }
}
