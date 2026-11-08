package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation

/**
 * Finds the files `/include` names
 */
fun interface A2lFileResolver {
    /**
     * @param path As written after `/include`
     * @param from The file the `/include` is in, so relative paths can be resolved against it
     * @return The file's bytes, or null when it can't be found
     */
    fun read(path: String, from: String): ByteArray?
}

/**
 * Parses A2L text into an [A2lTree], with [A2lSpec] saying what each keyword takes. Lenient, the way real files need:
 * an unknown keyword is kept with a warning, a block missing its `/end` is closed where the file (or its parent)
 * ends, and nothing throws. Everything wrong goes into [diagnostics] with its line and column.
 * @param file The file's name, for diagnostics and resolving `/include`
 */
class A2lParser(
    private val text: String,
    private val file: String = "",
    private val resolver: A2lFileResolver? = null,
    private val includeStack: List<String> = emptyList()
) {
    private val lexer = A2lLexer(text, file)
    private val found = mutableListOf<Diagnostic>()

    /** How many blocks deep the parser is, so a file nested absurdly deep can't overflow the stack */
    private var depth = 0

    val diagnostics: List<Diagnostic> get() = lexer.diagnostics + found

    fun parse(): A2lTree = A2lTree(parseBody(open = null))

    /**
     * Elements up to the `/end` of [open] (its keyword), or the end of the file at the top level
     */
    private fun parseBody(open: A2lToken?): List<A2lElement> {
        val elements = mutableListOf<A2lElement>()
        while (true) {
            val token = lexer.peek()
            when (token.kind) {
                TokenKind.EOF -> {
                    if (open != null) error(open, "/begin ${open.text} isn't closed: the file ends first")
                    return elements
                }
                TokenKind.END -> {
                    lexer.next()
                    val name = lexer.peek().takeIf { it.kind == TokenKind.IDENT }?.also { lexer.next() }
                    if (open == null) {
                        warning(token, "/end ${name?.text.orEmpty()} without a /begin, skipped")
                        continue
                    }
                    if (name == null) warning(token, "/end without a keyword, taken as /end ${open.text}")
                    else if (name.text != open.text) error(name, "/end ${name.text} doesn't match /begin ${open.text} (line ${open.line}), taken as /end ${open.text}")
                    return elements
                }
                TokenKind.BEGIN -> {
                    lexer.next()
                    val keyword = lexer.peek()
                    if (keyword.kind != TokenKind.IDENT) {
                        error(keyword, "/begin isn't followed by a keyword")
                        continue
                    }
                    lexer.next()
                    elements += parseBlock(keyword)
                }
                TokenKind.INCLUDE -> {
                    lexer.next()
                    elements += parseInclude(token)
                }
                TokenKind.IDENT -> {
                    lexer.next()
                    elements += parseKeyword(token)
                }
                TokenKind.STRING, TokenKind.NUMBER -> {
                    lexer.next()
                    warning(token, "Unexpected value ${token.text}${open?.let { " in ${it.text}" }.orEmpty()}, skipped")
                }
                TokenKind.OTHER -> {
                    lexer.next()
                    warning(token, "Unexpected character '${token.text}', skipped")
                }
            }
        }
    }

    private fun parseBlock(keyword: A2lToken): A2lElement {
        val rule = A2lSpec.rule(keyword.text)
        val location = keyword.location(file)
        if (depth >= MAX_NESTING) {
            // Skipped without recursing: the lexer steps over everything up to the matching /end
            error(keyword, "/begin ${keyword.text} is nested more than $MAX_NESTING blocks deep, skipped")
            lexer.rawUntilEnd()
            if (lexer.peek().kind == TokenKind.IDENT) lexer.next()
            return A2lElement(keyword.text, true, emptyList(), emptyList(), location, known = false)
        }
        depth++
        try {
            return parseBlockContent(keyword, rule, location)
        } finally {
            depth--
        }
    }

    private fun parseBlockContent(keyword: A2lToken, rule: A2lRule?, location: SourceLocation): A2lElement {
        if (rule != null && rule.raw) {
            val raw = lexer.rawUntilEnd()
            if (raw == null) {
                error(keyword, "/begin ${keyword.text} isn't closed: the file ends first")
                return A2lElement(keyword.text, true, emptyList(), emptyList(), location, raw = "")
            }
            val end = lexer.peek()
            if (end.kind == TokenKind.IDENT) {
                lexer.next()
                if (end.text != keyword.text) error(end, "/end ${end.text} doesn't match /begin ${keyword.text} (line ${keyword.line})")
            }
            return A2lElement(keyword.text, true, emptyList(), emptyList(), location, raw = raw)
        }
        if (rule == null || !rule.block) {
            warning(keyword, "Unknown block ${keyword.text}, kept as it is")
            val values = readValuesToKeyword()
            return A2lElement(keyword.text, true, values, parseBody(keyword), location, known = false)
        }
        val values = readParams(keyword, rule)
        return A2lElement(keyword.text, true, values, parseBody(keyword), location)
    }

    private fun parseKeyword(keyword: A2lToken): A2lElement {
        val rule = A2lSpec.rule(keyword.text)
        val location = keyword.location(file)
        if (rule == null || rule.block) {
            warning(keyword, if (rule == null) "Unknown keyword ${keyword.text}, kept as it is" else "${keyword.text} should be a /begin block, read as a keyword")
            return A2lElement(keyword.text, false, readValuesToKeyword(), emptyList(), location, known = rule != null)
        }
        return A2lElement(keyword.text, false, readParams(keyword, rule), emptyList(), location)
    }

    /** The parameters [rule] says [keyword] has */
    private fun readParams(keyword: A2lToken, rule: A2lRule): List<A2lValue> {
        val values = mutableListOf<A2lValue>()
        var missing = false
        for (part in rule.params) {
            when (part) {
                is ParamPart.One -> {
                    val token = lexer.peek()
                    val fits = token.isValue() && !(part.kind != ValueKind.IDENT && part.kind != ValueKind.ANY && token.isKeyword())
                    if (!fits || (part.optional && !token.matches(part.kind))) {
                        if (!part.optional) missing = true
                        if (missing) break else continue
                    }
                    lexer.next()
                    if (!token.matches(part.kind)) {
                        warning(token, "${keyword.text}: expected ${part.kind.label}, found ${token.text}")
                    }
                    values += token.toValue()
                }
                is ParamPart.Many -> while (lexer.peek().matches(part.kind)) values += lexer.next().toValue()
                is ParamPart.NamesToKeyword -> while (lexer.peek().isValue() && !lexer.peek().isKeyword()) values += lexer.next().toValue()
                is ParamPart.AllToEnd -> while (lexer.peek().isValue()) values += lexer.next().toValue()
                is ParamPart.Group -> while (lexer.peek().matches(part.kinds.first())) {
                    for (kind in part.kinds) {
                        val token = lexer.peek()
                        if (!token.isValue()) {
                            error(token, "${keyword.text}: a ${part.kinds.joinToString("/") { it.label }} entry is cut short")
                            break
                        }
                        lexer.next()
                        if (!token.matches(kind)) warning(token, "${keyword.text}: expected ${kind.label}, found ${token.text}")
                        values += token.toValue()
                    }
                }
            }
        }
        if (missing) {
            error(keyword, "${keyword.text} is missing parameters: it has ${values.size} of ${rule.params.count { !it.optional }}")
        }
        return values
    }

    /** Values up to the next keyword, for keywords and blocks [A2lSpec] doesn't know */
    private fun readValuesToKeyword(): List<A2lValue> {
        val values = mutableListOf<A2lValue>()
        while (true) {
            val token = lexer.peek()
            if (!token.isValue() || token.isKeyword()) return values
            // An unknown keyword's parameters are often followed by another unknown keyword: an upper case name
            // after at least one value is taken as the next keyword
            if (values.isNotEmpty() && token.kind == TokenKind.IDENT && token.looksLikeKeyword()) return values
            values += lexer.next().toValue()
        }
    }

    private fun parseInclude(include: A2lToken): List<A2lElement> {
        val pathToken = lexer.peek()
        if (pathToken.kind != TokenKind.STRING && pathToken.kind != TokenKind.IDENT) {
            error(include, "/include isn't followed by a file name")
            return emptyList()
        }
        lexer.next()
        val path = pathToken.text
        if (resolver == null) {
            warning(pathToken, "/include \"$path\" skipped: there's no folder to look for it in")
            return emptyList()
        }
        if (path in includeStack || path == file || includeStack.size >= MAX_INCLUDE_DEPTH) {
            error(pathToken, "/include \"$path\" includes itself, skipped")
            return emptyList()
        }
        val bytes = resolver.read(path, file)
        if (bytes == null) {
            error(pathToken, "/include \"$path\" couldn't be found")
            return emptyList()
        }
        val included = A2lParser(A2lText.decode(bytes), path, resolver, includeStack + file)
        val tree = included.parse()
        found += included.diagnostics
        return tree.elements
    }

    private fun A2lToken.isValue() = kind == TokenKind.IDENT || kind == TokenKind.STRING || kind == TokenKind.NUMBER

    private fun A2lToken.isKeyword() = kind == TokenKind.IDENT && A2lSpec.rule(text) != null

    private fun A2lToken.looksLikeKeyword() = text.length > 1 && text.all { it.isUpperCase() || it == '_' || it.isDigit() } && text.first().isLetter()

    private fun A2lToken.matches(kind: ValueKind): Boolean = when (kind) {
        ValueKind.IDENT -> this.kind == TokenKind.IDENT
        ValueKind.STRING -> this.kind == TokenKind.STRING
        ValueKind.NUMBER -> this.kind == TokenKind.NUMBER
        ValueKind.ANY -> isValue()
    }

    private fun A2lToken.toValue(): A2lValue {
        val kind = when (kind) {
            TokenKind.STRING -> ValueKind.STRING
            TokenKind.NUMBER -> ValueKind.NUMBER
            else -> ValueKind.IDENT
        }
        return A2lValue(kind, text, location(file))
    }

    private fun error(token: A2lToken, message: String) {
        found += Diagnostic(Severity.ERROR, message, token.location(file))
    }

    private fun warning(token: A2lToken, message: String) {
        found += Diagnostic(Severity.WARNING, message, token.location(file))
    }

    private val ValueKind.label: String
        get() = when (this) {
            ValueKind.IDENT -> "a name"
            ValueKind.STRING -> "a string"
            ValueKind.NUMBER -> "a number"
            ValueKind.ANY -> "a value"
        }

    companion object {
        const val MAX_INCLUDE_DEPTH = 16

        /** Real files nest about 6 deep; this is far past that and far short of the stack running out */
        const val MAX_NESTING = 256

        /** Where a [SourceLocation] would point for a whole file */
        fun fileLocation(file: String) = SourceLocation(file, 1, 1)
    }
}
