package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import kotlinx.serialization.Serializable

@Serializable
enum class CharacteristicType {
    ASCII,
    CURVE,
    MAP,
    CUBOID,
    CUBE_4,
    CUBE_5,
    VAL_BLK,
    VALUE,
    NOT_SET;

    override fun toString(): String {
        return name
    }
}

fun String.parseCharacteristicType(): CharacteristicType {
    return when (this.trim()) {
        "ASCII" -> CharacteristicType.ASCII
        "CURVE" -> CharacteristicType.CURVE
        "MAP" -> CharacteristicType.MAP
        "CUBOID" -> CharacteristicType.CUBOID
        "CUBE_4" -> CharacteristicType.CUBE_4
        "CUBE_5" -> CharacteristicType.CUBE_5
        "VAL_BLK" -> CharacteristicType.VAL_BLK
        "VALUE" -> CharacteristicType.VALUE
        else -> CharacteristicType.NOT_SET
    }
}