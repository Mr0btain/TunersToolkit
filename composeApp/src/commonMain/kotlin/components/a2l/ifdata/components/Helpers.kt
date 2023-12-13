package components.a2l.ifdata.components

import components.a2l.ifdata.IfData
import components.a2l.ifdata.IfDataBuilder

fun List<String>.parseIfDataSection(): IfData {
    var found = false
    var result = ""
    this.forEachIndexed { index, line ->
        when {
            !found && line.contains("/end IF_DATA") -> {
                result = this.subList(0, index).joinToString(" ")
                found = true
            }
        }
    }
    return IfDataBuilder().build(result)
}