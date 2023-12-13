package components.parsers.a2l.ifdata.components

import components.parsers.a2l.ifdata.buildIfData

fun List<String>.parseIfDataSection(): components.parsers.a2l.ifdata.IfData {
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
    return buildIfData(result)
}