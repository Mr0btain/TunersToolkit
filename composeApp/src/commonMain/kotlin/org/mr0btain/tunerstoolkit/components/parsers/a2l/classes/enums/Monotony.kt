package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class Monotony : A2lOptional {
    MON_DECREASE,
    MON_INCREASE,
    STRICT_DECREASE,
    STRICT_INCREASE,
    MONOTONOUS,
    STRICT_MON,
    NOT_MON,
    NOT_SET;
}

fun String.parseMonotony(): Monotony {
    return when (this.trim()) {
        "MON_DECREASE" -> Monotony.MON_DECREASE
        "MON_INCREASE" -> Monotony.MON_INCREASE
        "STRICT_DECREASE" -> Monotony.STRICT_DECREASE
        "STRICT_INCREASE" -> Monotony.STRICT_INCREASE
        "MONOTONOUS" -> Monotony.MONOTONOUS
        "STRICT_MON" -> Monotony.STRICT_MON
        "NOT_MON" -> Monotony.NOT_MON
        else -> Monotony.NOT_SET
    }
}