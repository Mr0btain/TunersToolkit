package org.mr0btain.tunerstoolkit.core.formats.xml

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation

/**
 * An XML element: its name, attributes, child elements and text (all the text directly in it, joined). Enough for
 * the definition and value formats (XDF, CDFX, PaCo); no namespaces, schemas or DTDs
 */
class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
    val text: String,
    val line: Int
) {
    fun child(name: String): XmlElement? = children.firstOrNull { it.name.equals(name, ignoreCase = true) }
    fun children(name: String): List<XmlElement> = children.filter { it.name.equals(name, ignoreCase = true) }

    /** A child's text, trimmed; null when there's no such child */
    fun childText(name: String): String? = child(name)?.text?.trim()

    /** An attribute, by name in any case */
    fun attribute(name: String): String? = attributes[name] ?: attributes.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    /** Every element in this one, depth first, this one included */
    fun walk(): Sequence<XmlElement> = sequence {
        yield(this@XmlElement)
        children.forEach { yieldAll(it.walk()) }
    }

    override fun toString(): String = "<$name> line $line"
}

/**
 * Reads XML leniently: a tag that isn't closed is closed where its parent ends, a stray end tag is skipped, unknown
 * entities are kept as written, and everything wrong is a diagnostic rather than an exception. Nesting deeper than
 * [MAX_DEPTH] is cut off, so a hostile file can't overflow the stack
 */
class XmlParser(private val text: String, private val file: String = "") {
    private var position = 0
    private var line = 1
    private val found = mutableListOf<Diagnostic>()
    val diagnostics: List<Diagnostic> get() = found

    /** The root element, or null when there's none */
    fun parse(): XmlElement? {
        val roots = mutableListOf<XmlElement>()
        while (true) {
            skipMisc()
            if (position >= text.length) break
            if (text.startsWith("</", position)) {
                warn("A stray </ … > before the first element, skipped")
                skipPast(">")
                continue
            }
            if (text[position] != '<') {
                position++
                continue
            }
            element(0)?.let { roots += it }
        }
        if (roots.size > 1) warn("More than one top level element; only the first is used")
        return roots.firstOrNull()
    }

    /** Comments, processing instructions, DOCTYPE and whitespace between elements */
    private fun skipMisc() {
        while (position < text.length) {
            when {
                text[position].isWhitespace() -> advance(1)
                text.startsWith("<?", position) -> skipPast("?>")
                text.startsWith("<!--", position) -> skipPast("-->")
                text.startsWith("<!DOCTYPE", position, ignoreCase = true) -> skipDoctype()
                else -> return
            }
        }
    }

    private fun skipDoctype() {
        var depth = 0
        while (position < text.length) {
            val c = text[position]
            advance(1)
            if (c == '[') depth++
            if (c == ']') depth--
            if (c == '>' && depth <= 0) return
        }
    }

    /** Reads the element starting at '<'; returns null when it can't */
    private fun element(depth: Int): XmlElement? {
        val startLine = line
        advance(1) // <
        val name = name()
        if (name.isEmpty()) {
            warn("A '<' that doesn't start a tag, skipped")
            return null
        }
        val attributes = linkedMapOf<String, String>()
        while (true) {
            skipSpace()
            if (position >= text.length) {
                warn("<$name> isn't closed: the file ends first")
                return XmlElement(name, attributes, emptyList(), "", startLine)
            }
            if (text.startsWith("/>", position)) {
                advance(2)
                return XmlElement(name, attributes, emptyList(), "", startLine)
            }
            if (text[position] == '>') {
                advance(1)
                break
            }
            val key = name()
            if (key.isEmpty()) {
                advance(1)
                continue
            }
            skipSpace()
            if (position < text.length && text[position] == '=') {
                advance(1)
                skipSpace()
                attributes[key] = attributeValue()
            } else {
                attributes[key] = ""
            }
        }
        if (depth >= MAX_DEPTH) {
            warn("<$name> is nested more than $MAX_DEPTH deep, its content is skipped")
            skipToEnd(name)
            return XmlElement(name, attributes, emptyList(), "", startLine)
        }
        val children = mutableListOf<XmlElement>()
        val content = StringBuilder()
        while (position < text.length) {
            when {
                text.startsWith("</", position) -> {
                    advance(2)
                    val end = name()
                    skipPast(">")
                    if (!end.equals(name, ignoreCase = true)) warn("</$end> doesn't match <$name> (line $startLine)")
                    return XmlElement(name, attributes, children, content.toString(), startLine)
                }
                text.startsWith("<!--", position) -> skipPast("-->")
                text.startsWith("<![CDATA[", position) -> {
                    advance(9)
                    val end = text.indexOf("]]>", position).let { if (it < 0) text.length else it }
                    content.append(text, position, end)
                    advance(end - position)
                    advance(3)
                }
                text.startsWith("<?", position) -> skipPast("?>")
                text[position] == '<' -> element(depth + 1)?.let { children += it }
                text[position] == '&' -> content.append(entity())
                else -> {
                    content.append(text[position])
                    advance(1)
                }
            }
        }
        warn("<$name> isn't closed: the file ends first")
        return XmlElement(name, attributes, children, content.toString(), startLine)
    }

    /** Steps over everything up to the end tag matching [name], counting nesting without recursing */
    private fun skipToEnd(name: String) {
        var depth = 1
        while (position < text.length && depth > 0) {
            when {
                text.startsWith("</", position) -> {
                    advance(2)
                    if (name().equals(name, ignoreCase = true)) depth--
                    skipPast(">")
                }
                text.startsWith("<!--", position) -> skipPast("-->")
                text[position] == '<' -> {
                    advance(1)
                    val inner = name()
                    val close = text.indexOf('>', position).let { if (it < 0) text.length else it }
                    val selfClosing = close > 0 && text[close - 1] == '/'
                    advance(close - position + 1)
                    if (inner.equals(name, ignoreCase = true) && !selfClosing) depth++
                }
                else -> advance(1)
            }
        }
    }

    private fun attributeValue(): String {
        if (position >= text.length) return ""
        val quote = text[position]
        if (quote != '"' && quote != '\'') return name()
        advance(1)
        val value = StringBuilder()
        while (position < text.length && text[position] != quote) {
            if (text[position] == '&') value.append(entity()) else {
                value.append(text[position])
                advance(1)
            }
        }
        advance(1)
        return value.toString()
    }

    private fun entity(): String {
        val end = text.indexOf(';', position)
        if (end < 0 || end - position > 12) {
            advance(1)
            return "&"
        }
        val name = text.substring(position + 1, end)
        val value = when {
            name == "lt" -> "<"
            name == "gt" -> ">"
            name == "amp" -> "&"
            name == "quot" -> "\""
            name == "apos" -> "'"
            name.startsWith("#x") || name.startsWith("#X") -> name.drop(2).toIntOrNull(16)?.let(::codePoint)
            name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePoint)
            else -> null
        }
        if (value == null) {
            advance(1)
            return "&"
        }
        advance(end - position + 1)
        return value
    }

    private fun codePoint(code: Int): String? = when {
        code < 0 || code > 0x10FFFF -> null
        code < 0x10000 -> code.toChar().toString()
        else -> {
            val shifted = code - 0x10000
            charArrayOf((0xD800 + (shifted shr 10)).toChar(), (0xDC00 + (shifted and 0x3FF)).toChar()).concatToString()
        }
    }

    private fun name(): String {
        val start = position
        while (position < text.length && (text[position].isLetterOrDigit() || text[position] in "_-.:")) position++
        return text.substring(start, position)
    }

    private fun skipSpace() {
        while (position < text.length && text[position].isWhitespace()) advance(1)
    }

    private fun skipPast(marker: String) {
        val end = text.indexOf(marker, position)
        val to = if (end < 0) text.length else end + marker.length
        advance(to - position)
    }

    private fun advance(count: Int) {
        val to = (position + count).coerceAtMost(text.length)
        for (i in position until to) if (text[i] == '\n') line++
        position = to
    }

    private fun warn(message: String) {
        found += Diagnostic(Severity.WARNING, message, SourceLocation(file, line))
    }

    companion object {
        const val MAX_DEPTH = 256

        fun parse(text: String, file: String = ""): Pair<XmlElement?, List<Diagnostic>> {
            val parser = XmlParser(text, file)
            return parser.parse() to parser.diagnostics
        }
    }
}

/** Builds indented XML text */
class XmlWriter {
    private val out = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    private var depth = 0

    fun raw(line: String) = apply { out.append(line).append('\n') }

    /** `<name attributes>`, then [content] inside, then `</name>` */
    fun element(name: String, vararg attributes: Pair<String, Any?>, content: XmlWriter.() -> Unit) = apply {
        indent()
        out.append('<').append(name).appendAttributes(attributes).append(">\n")
        depth++
        content()
        depth--
        indent()
        out.append("</").append(name).append(">\n")
    }

    /** `<name attributes>text</name>`, or `<name attributes/>` without text */
    fun leaf(name: String, text: String? = null, vararg attributes: Pair<String, Any?>) = apply {
        indent()
        out.append('<').append(name).appendAttributes(attributes)
        if (text == null) out.append("/>\n") else out.append('>').append(escape(text)).append("</").append(name).append(">\n")
    }

    private fun StringBuilder.appendAttributes(attributes: Array<out Pair<String, Any?>>): StringBuilder {
        for ((key, value) in attributes) if (value != null) append(' ').append(key).append("=\"").append(escape(value.toString())).append('"')
        return this
    }

    private fun indent() {
        repeat(depth) { out.append("  ") }
    }

    override fun toString(): String = out.toString()

    companion object {
        fun escape(text: String): String = buildString(text.length) {
            for (c in text) when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                else -> append(c)
            }
        }
    }
}
