package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class Root: A2lOptional {
    True,
    False
}