package org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.buildIfData

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
    return buildIfData(result).first
}