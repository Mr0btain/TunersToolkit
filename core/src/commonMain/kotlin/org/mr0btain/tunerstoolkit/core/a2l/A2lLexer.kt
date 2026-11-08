package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation

/** What kind of thing a [A2lToken] is */
enum class TokenKind {
    /** A name or keyword: letters, digits, `_`, `.` and `[]` (A2L names can be like `map.x[2]`) */
    IDENT,
    STRING,
    NUMBER,
    /** `/begin` */
    BEGIN,
    /** `/end` */
    END,
    /** `/include` */
    INCLUDE,
    /** A character A2L has no use for, kept so the parser can report it */
    OTHER,
    EOF
}

/**
 * One token, with where it starts in the file
 * @property text The token as written; for a [TokenKind.STRING] the value with its quotes removed and escapes undone
 * @property offset Index of its first character in the source text
 */
class A2lToken(
    val kind: TokenKind,
    val text: String,
    val line: Int,
    val column: Int,
    val offset: Int
) {
    fun location(file: String): SourceLocation = SourceLocation(file, line, column)

    override fun toString(): String = "$kind($text)@$line:$column"
}

/**
 * Splits A2L text into tokens, one at a time, so a big file is never held as a token list. Never throws: anything it
 * can't read becomes an [TokenKind.OTHER] token or a [Diagnostic], and it carries on.
 *
 * Handles `/* */` and `//` comments, strings with `\"`, `""`, `\\`, `\n` and `\t`, decimal, hex and float numbers
 * (with signs and exponents), and `/begin`, `/end` and `/include` in any case.
 */
class A2lLexer(private val text: String, private val source: String = "") {
    private var index = 0
    private var line = 1
    private var lineStart = 0
    private var peeked: A2lToken? = null

    val diagnostics = mutableListOf<Diagnostic>()

    /** The next token, without taking it */
    fun peek(): A2lToken = peeked ?: read().also { peeked = it }

    /** Takes the next token */
    fun next(): A2lToken {
        val token = peek()
        peeked = null
        return token
    }

    /**
     * The source text from here up to (not including) the `/end` that matches a `/begin` already read, and that
     * `/end` itself, which is skipped. For A2ML, whose grammar isn't A2L tokens. Strings and comments are stepped
     * over, so an `/end` inside them doesn't count
     * @return The raw text, or null when the file ends first
     */
    fun rawUntilEnd(): String? {
        check(peeked == null) { "rawUntilEnd after peek" }
        val start = index
        var depth = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' -> skipString()
                char == '/' && next(1) == '*' -> skipBlockComment()
                char == '/' && next(1) == '/' -> skipLineComment()
                char == '/' && matchesWord("begin") -> { depth++; index += 6 }
                char == '/' && matchesWord("end") -> {
                    if (depth == 0) {
                        val raw = text.substring(start, index)
                        index += 4
                        return raw
                    }
                    depth--
                    index += 4
                }
                else -> advance()
            }
        }
        return null
    }

    private fun read(): A2lToken {
        skipBlanksAndComments()
        if (index >= text.length) return token(TokenKind.EOF, "", index)
        val start = index
        val char = text[index]
        return when {
            char == '"' -> readString()
            char == '/' -> readSlash()
            isNumberStart() -> readNumber()
            isIdentStart(char) -> readIdent()
            else -> {
                advance()
                token(TokenKind.OTHER, char.toString(), start)
            }
        }
    }

    private fun readSlash(): A2lToken {
        val start = index
        for ((word, kind) in SLASH_WORDS) {
            if (matchesWord(word)) {
                index += word.length + 1
                return token(kind, "/$word", start)
            }
        }
        advance()
        return token(TokenKind.OTHER, "/", start)
    }

    /** Whether `/` + [word] (any case) is here, not followed by more of a name */
    private fun matchesWord(word: String): Boolean {
        val end = index + 1 + word.length
        if (end > text.length) return false
        if (!text.regionMatches(index + 1, word, 0, word.length, ignoreCase = true)) return false
        return end == text.length || !isIdentPart(text[end])
    }

    private fun readString(): A2lToken {
        val start = index
        val startLine = line
        val startColumn = index - lineStart + 1
        advance() // opening quote
        val value = StringBuilder()
        while (true) {
            if (index >= text.length) {
                diagnostics += Diagnostic(Severity.ERROR, "A string isn't closed before the end of the file", SourceLocation(source, startLine, startColumn))
                break
            }
            val char = text[index]
            when {
                char == '\\' && index + 1 < text.length -> {
                    when (val escaped = text[index + 1]) {
                        'n' -> value.append('\n')
                        't' -> value.append('\t')
                        'r' -> value.append('\r')
                        '"', '\\', '\'' -> value.append(escaped)
                        else -> value.append('\\').append(escaped)
                    }
                    advance(); advance()
                }
                char == '"' && next(1) == '"' -> {
                    value.append('"')
                    advance(); advance()
                }
                char == '"' -> {
                    advance()
                    break
                }
                else -> {
                    value.append(char)
                    advance()
                }
            }
        }
        return A2lToken(TokenKind.STRING, value.toString(), startLine, startColumn, start)
    }

    private fun isNumberStart(): Boolean {
        val char = text[index]
        if (char.isDigit()) return true
        if (char == '.' && next(1)?.isDigit() == true) return true
        if (char == '+' || char == '-') {
            val after = next(1) ?: return false
            return after.isDigit() || (after == '.' && next(2)?.isDigit() == true)
        }
        return false
    }

    private fun readNumber(): A2lToken {
        val start = index
        if (text[index] == '+' || text[index] == '-') advance()
        if (text[index] == '0' && (next(1) == 'x' || next(1) == 'X')) {
            advance(); advance()
            while (index < text.length && text[index].isHexDigit()) advance()
        } else {
            while (index < text.length && text[index].isDigit()) advance()
            if (index < text.length && text[index] == '.') {
                advance()
                while (index < text.length && text[index].isDigit()) advance()
            }
            if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
                val save = index
                advance()
                if (index < text.length && (text[index] == '+' || text[index] == '-')) advance()
                if (index < text.length && text[index].isDigit()) {
                    while (index < text.length && text[index].isDigit()) advance()
                } else {
                    index = save
                }
            }
        }
        // Something like 12abc is a name that starts with digits, which some tools write
        if (index < text.length && isIdentPart(text[index])) {
            while (index < text.length && isIdentPart(text[index])) advance()
            return token(TokenKind.IDENT, text.substring(start, index), start)
        }
        return token(TokenKind.NUMBER, text.substring(start, index), start)
    }

    private fun readIdent(): A2lToken {
        val start = index
        while (index < text.length && isIdentPart(text[index])) advance()
        return token(TokenKind.IDENT, text.substring(start, index), start)
    }

    private fun skipBlanksAndComments() {
        while (index < text.length) {
            val char = text[index]
            when {
                char == '\n' -> newLine()
                char.isWhitespace() || char == '﻿' -> index++
                char == '/' && next(1) == '*' -> skipBlockComment()
                char == '/' && next(1) == '/' -> skipLineComment()
                else -> return
            }
        }
    }

    private fun skipBlockComment() {
        val startLine = line
        val startColumn = index - lineStart + 1
        index += 2
        while (index < text.length) {
            if (text[index] == '*' && next(1) == '/') {
                index += 2
                return
            }
            advance()
        }
        diagnostics += Diagnostic(Severity.WARNING, "A comment isn't closed before the end of the file", SourceLocation(source, startLine, startColumn))
    }

    private fun skipLineComment() {
        while (index < text.length && text[index] != '\n') index++
    }

    private fun skipString() {
        advance()
        while (index < text.length) {
            val char = text[index]
            if (char == '\\') { advance(); if (index < text.length) advance(); continue }
            if (char == '"') {
                if (next(1) == '"') { advance(); advance(); continue }
                advance()
                return
            }
            advance()
        }
    }

    private fun advance() {
        if (text[index] == '\n') newLine() else index++
    }

    private fun newLine() {
        index++
        line++
        lineStart = index
    }

    private fun next(ahead: Int): Char? = text.getOrNull(index + ahead)

    private fun token(kind: TokenKind, value: String, start: Int): A2lToken {
        // Tokens other than strings never span lines, so the column comes from the current line
        val column = start - lineStart + 1
        return A2lToken(kind, value, line, if (column > 0) column else 1, start)
    }

    companion object {
        private val SLASH_WORDS = listOf("begin" to TokenKind.BEGIN, "end" to TokenKind.END, "include" to TokenKind.INCLUDE)

        fun isIdentStart(char: Char): Boolean = char.isLetter() || char == '_' || char == '.' || char == '['
        fun isIdentPart(char: Char): Boolean = char.isLetterOrDigit() || char == '_' || char == '.' || char == '[' || char == ']'

        private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
