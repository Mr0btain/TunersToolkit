package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import kotlinx.serialization.Serializable

@Serializable
enum class IndexIncrement {
    INDEX_INCR,
    INDEX_DECR,
    NOT_SET;
}

fun String.parseIndexIncrement(): IndexIncrement {
    return when (this.trim()) {
        "INDEX_INCR" -> IndexIncrement.INDEX_INCR
        "INDEX_DECR" -> IndexIncrement.INDEX_DECR
        else -> IndexIncrement.NOT_SET
    }
}