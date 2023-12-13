package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * One ANNOTATION may represent a voluminous description. Its purpose is to be e.g.
 * an application note which explains the function of an identifier for the calibration
 * engineer.
 */

@Serializable
data class Annotation(
    val label: String,
    val origin: String,
    val text: String
): A2lOptional

/**
 * Parses a list of strings and returns an Annotation if it contains one
 */

fun List<String>.parseAnnotation(): Annotation {
    var label = ""
    var origin = ""
    var text = ""

    this.forEachIndexed { index, it ->
        when {
            it.startsWith("ANNOTATION_LABEL") -> {
                label = it.replace("ANNOTATION_LABEL", "").trim()
            }

            it.startsWith("ANNOTATION_ORIGIN") -> {
                origin = it.replace("ANNOTATION_ORIGIN", "").trim()
            }

            it.startsWith("/begin ANNOTATION_TEXT") -> {
                val subList = this.subList(index, this.size)
                var result = ""
                subList.forEachIndexed { ind, s ->
                    if (s.startsWith("/end ANNOTATION_TEXT")) {
                        result = subList.subList(0, ind).joinToString(" ")
                            .replace("/begin ANNOTATION_TEXT", "")
                            .replace("/end ANNOTATION_TEXT", "")
                            .replace("\"", "")
                            .trim()
                    }
                }
                text = result
            }
        }
    }

    return Annotation(
        label = label,
        origin = origin,
        text = text
    )
}