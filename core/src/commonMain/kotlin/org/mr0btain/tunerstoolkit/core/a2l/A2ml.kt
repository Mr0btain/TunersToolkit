package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation

/**
 * A type in an A2ML grammar: what an IF_DATA block of some tool (XCP, CCP, ETK, …) may hold
 */
sealed interface A2mlType {
    /** char, int, long, uint, ulong, int64, uint64, float, double, float16 */
    data class Basic(val name: String) : A2mlType

    data class Struct(val name: String?, val members: List<Member>) : A2mlType

    data class TaggedStruct(val name: String?, val members: List<TaggedMember>) : A2mlType

    data class TaggedUnion(val name: String?, val members: List<TaggedMember>) : A2mlType

    data class Enum(val name: String?, val values: Map<String, Long?>) : A2mlType

    /** A named struct, taggedstruct, taggedunion or enum declared elsewhere in the grammar */
    data class Ref(val kind: String, val name: String) : A2mlType

    /** A struct member; [dimensions] for arrays like `uint[2]` */
    data class Member(val type: A2mlType, val dimensions: List<Long> = emptyList())

    /**
     * A taggedstruct or taggedunion member: `"TAG" type`, or a `block "TAG" type` (written with /begin and /end)
     * @property repeated Written `( ... )*`: it may appear any number of times
     * @property dimensions For a member like `"KEY" char[256]`
     */
    data class TaggedMember(
        val tag: String,
        val type: A2mlType?,
        val block: Boolean,
        val repeated: Boolean = false,
        val dimensions: List<Long> = emptyList()
    )
}

/**
 * A parsed `/begin A2ML` grammar
 * @property blocks The top level `block "TAG" ...` declarations; `IF_DATA` is the one that matters
 * @property types Named types, by name, for resolving [A2mlType.Ref]
 */
class A2ml(val blocks: List<A2mlType.TaggedMember>, val types: Map<String, A2mlType>) {
    /** The type a [A2mlType.Ref] names, or the type itself when it isn't a reference */
    fun resolve(type: A2mlType?): A2mlType? = if (type is A2mlType.Ref) types[type.name] else type

    /** The tags IF_DATA may have (XCP, CANAPE_EXT, …), from `block "IF_DATA" taggedunion { ... }` */
    val ifDataTags: Set<String>
        get() {
            val ifData = blocks.firstOrNull { it.tag == "IF_DATA" } ?: return emptySet()
            return when (val type = resolve(ifData.type)) {
                is A2mlType.TaggedUnion -> type.members.map { it.tag }.toSet()
                is A2mlType.TaggedStruct -> type.members.map { it.tag }.toSet()
                else -> emptySet()
            }
        }

    companion object {
        /**
         * Parses A2ML [text]. Never throws: what can't be read is reported and skipped to the next `;`
         * @param location Where the A2ML block starts, so diagnostics point into the file
         */
        fun parse(text: String, location: SourceLocation? = null): Pair<A2ml, List<Diagnostic>> {
            val parser = A2mlParser(text, location)
            return parser.parse() to parser.diagnostics
        }
    }
}

private class A2mlParser(private val text: String, private val origin: SourceLocation?) {
    private data class Tok(val kind: Char, val text: String, val line: Int)

    private val tokens = tokenize()
    private var position = 0
    private val types = mutableMapOf<String, A2mlType>()
    val diagnostics = mutableListOf<Diagnostic>()

    fun parse(): A2ml {
        val blocks = mutableListOf<A2mlType.TaggedMember>()
        while (peek() != null) {
            val start = position
            try {
                if (peek()?.text == "block") {
                    blocks += blockDefinition(repeated = false)
                } else {
                    typeName()
                }
                expect(";")
            } catch (e: A2mlSyntaxError) {
                report(e.message ?: "A2ML can't be read here", e.line)
                // Skip past the next ';' so one bad declaration doesn't stop the rest
                while (peek() != null && peek()?.text != ";") position++
                position++
                if (position <= start) position = start + 1
            }
        }
        return A2ml(blocks, types)
    }

    private fun blockDefinition(repeated: Boolean): A2mlType.TaggedMember {
        expect("block")
        val tag = string()
        return A2mlType.TaggedMember(tag, typeName(), block = true, repeated = repeated)
    }

    private fun typeName(): A2mlType {
        val token = take() ?: throw A2mlSyntaxError("A2ML ends where a type was expected", lastLine())
        return when (token.text) {
            in BASIC_TYPES -> A2mlType.Basic(token.text)
            "struct" -> named("struct") { name ->
                val members = mutableListOf<A2mlType.Member>()
                while (!accept("}")) {
                    val type = typeName()
                    val dimensions = arrayDimensions()
                    expect(";")
                    members += A2mlType.Member(type, dimensions)
                }
                A2mlType.Struct(name, members)
            }
            "taggedstruct" -> named("taggedstruct") { name -> A2mlType.TaggedStruct(name, taggedMembers()) }
            "taggedunion" -> named("taggedunion") { name -> A2mlType.TaggedUnion(name, taggedMembers()) }
            "enum" -> named("enum") { name ->
                val values = linkedMapOf<String, Long?>()
                do {
                    if (peek()?.text == "}") break
                    val keyword = string()
                    values[keyword] = if (accept("=")) number() else null
                } while (accept(","))
                expect("}")
                A2mlType.Enum(name, values)
            }
            else -> throw A2mlSyntaxError("Unknown A2ML type ${token.text}", token.line)
        }
    }

    /** `kind [name] [{ body }]`: a declaration with a body, or a reference to a named one without */
    private fun named(kind: String, body: (String?) -> A2mlType): A2mlType {
        val name = if (peek()?.kind == 'i') take()!!.text else null
        if (!accept("{")) {
            if (name == null) throw A2mlSyntaxError("$kind has neither a name nor a body", lastLine())
            return A2mlType.Ref(kind, name)
        }
        val type = body(name)
        if (name != null) types[name] = type
        return type
    }

    private fun taggedMembers(): List<A2mlType.TaggedMember> {
        val members = mutableListOf<A2mlType.TaggedMember>()
        while (!accept("}")) {
            val repeated = accept("(")
            val member = when {
                peek()?.text == "block" -> blockDefinition(repeated)
                else -> {
                    val tag = string()
                    // "TAG" (type)* : the type repeats; otherwise an optional type
                    val type = when {
                        peek()?.text == "(" -> { take(); typeName().also { expect(")"); expect("*") } }
                        peek()?.text == ";" || peek()?.text == ")" -> null
                        else -> typeName()
                    }
                    A2mlType.TaggedMember(tag, type, block = false, repeated = repeated, dimensions = arrayDimensions())
                }
            }
            if (repeated) {
                expect(")")
                expect("*")
            }
            expect(";")
            members += member
        }
        return members
    }

    private fun arrayDimensions(): List<Long> {
        val dimensions = mutableListOf<Long>()
        while (accept("[")) {
            dimensions += number()
            expect("]")
        }
        return dimensions
    }

    private fun string(): String {
        val token = take() ?: throw A2mlSyntaxError("A2ML ends where a \"tag\" was expected", lastLine())
        if (token.kind != 's') throw A2mlSyntaxError("Expected a \"tag\", found ${token.text}", token.line)
        return token.text
    }

    private fun number(): Long {
        val token = take() ?: throw A2mlSyntaxError("A2ML ends where a number was expected", lastLine())
        return A2lValue.parseLong(token.text) ?: throw A2mlSyntaxError("Expected a number, found ${token.text}", token.line)
    }

    private fun expect(text: String) {
        val token = take() ?: throw A2mlSyntaxError("A2ML ends where '$text' was expected", lastLine())
        if (token.text != text) throw A2mlSyntaxError("Expected '$text', found ${token.text}", token.line)
    }

    private fun accept(text: String): Boolean {
        if (peek()?.text != text || peek()?.kind == 's') return false
        position++
        return true
    }

    private fun peek(): Tok? = tokens.getOrNull(position)
    private fun take(): Tok? = tokens.getOrNull(position)?.also { position++ }
    private fun lastLine(): Int = tokens.lastOrNull()?.line ?: 1

    private fun report(message: String, line: Int) {
        val location = origin?.let { SourceLocation(it.file, it.line + line - 1, 1) }
        diagnostics += Diagnostic(Severity.WARNING, "A2ML: $message", location)
    }

    /** Names ('i'), strings ('s'), numbers ('n') and single punctuation characters ('p') */
    private fun tokenize(): List<Tok> {
        val result = mutableListOf<Tok>()
        var index = 0
        var line = 1
        while (index < text.length) {
            val char = text[index]
            when {
                char == '\n' -> { line++; index++ }
                char.isWhitespace() -> index++
                char == '/' && text.getOrNull(index + 1) == '*' -> {
                    val end = text.indexOf("*/", index + 2).let { if (it < 0) text.length else it + 2 }
                    line += text.substring(index, end).count { it == '\n' }
                    index = end
                }
                char == '/' && text.getOrNull(index + 1) == '/' -> {
                    while (index < text.length && text[index] != '\n') index++
                }
                char == '"' -> {
                    val end = text.indexOf('"', index + 1).let { if (it < 0) text.length else it }
                    result += Tok('s', text.substring(index + 1, end), line)
                    index = end + 1
                }
                char.isDigit() || (char == '-' && text.getOrNull(index + 1)?.isDigit() == true) -> {
                    val start = index
                    index++
                    while (index < text.length && (text[index].isLetterOrDigit())) index++
                    result += Tok('n', text.substring(start, index), line)
                }
                char.isLetter() || char == '_' -> {
                    val start = index
                    while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) index++
                    result += Tok('i', text.substring(start, index), line)
                }
                else -> {
                    result += Tok('p', char.toString(), line)
                    index++
                }
            }
        }
        return result
    }

    companion object {
        val BASIC_TYPES = setOf("char", "int", "long", "uint", "ulong", "int64", "uint64", "float", "double", "float16")
    }
}

private class A2mlSyntaxError(message: String, val line: Int) : Exception(message)
