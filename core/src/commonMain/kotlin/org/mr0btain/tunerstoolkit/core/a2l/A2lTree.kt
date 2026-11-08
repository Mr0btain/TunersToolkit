package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation

/** A parameter value as written in the file */
class A2lValue(val kind: ValueKind, val text: String, val location: SourceLocation) {
    /** As a whole number: decimal or 0x hex. Null when it isn't one */
    fun asLong(): Long? = parseLong(text)

    /** As a number, whole or not. Null when it isn't one */
    fun asDouble(): Double? = parseLong(text)?.toDouble() ?: text.toDoubleOrNull()

    override fun toString(): String = if (kind == ValueKind.STRING) "\"$text\"" else text

    companion object {
        fun parseLong(text: String): Long? {
            val negative = text.startsWith("-")
            val unsigned = text.removePrefix("-").removePrefix("+")
            val value = if (unsigned.startsWith("0x") || unsigned.startsWith("0X")) {
                unsigned.substring(2).toULongOrNull(16)?.toLong()
            } else {
                unsigned.toLongOrNull()
            } ?: return null
            return if (negative) -value else value
        }
    }
}

/**
 * One keyword with its parameters, and for a block what's inside it. The parser's output: it keeps everything in
 * the file, unknown keywords included, in order, so the model can be built from it and a writer can put it back.
 * @property known Whether the keyword is in [A2lSpec]; unknown ones are kept as they were written
 * @property raw For A2ML and IF_DATA: the text between `/begin KEYWORD` and `/end KEYWORD`, exactly as written
 */
class A2lElement(
    val keyword: String,
    val block: Boolean,
    val values: List<A2lValue>,
    val children: List<A2lElement>,
    val location: SourceLocation,
    val known: Boolean = true,
    val raw: String? = null
) {
    /** The first child with this keyword */
    fun child(keyword: String): A2lElement? = children.firstOrNull { it.keyword == keyword }

    fun children(keyword: String): List<A2lElement> = children.filter { it.keyword == keyword }

    fun has(keyword: String): Boolean = children.any { it.keyword == keyword }

    fun value(index: Int): A2lValue? = values.getOrNull(index)
    fun text(index: Int): String? = values.getOrNull(index)?.text
    fun long(index: Int): Long? = values.getOrNull(index)?.asLong()
    fun double(index: Int): Double? = values.getOrNull(index)?.asDouble()

    /** Every element in this one, depth first, this one included */
    fun walk(): Sequence<A2lElement> = sequence {
        yield(this@A2lElement)
        children.forEach { yieldAll(it.walk()) }
    }

    override fun toString(): String = buildString {
        append(keyword)
        values.forEach { append(' ').append(it) }
        if (children.isNotEmpty()) append(" {").append(children.size).append(" children}")
    }
}

/** A parsed file: its top level elements (ASAP2_VERSION, A2ML_VERSION and PROJECT, usually) */
class A2lTree(val elements: List<A2lElement>) {
    fun walk(): Sequence<A2lElement> = elements.asSequence().flatMap { it.walk() }
}
