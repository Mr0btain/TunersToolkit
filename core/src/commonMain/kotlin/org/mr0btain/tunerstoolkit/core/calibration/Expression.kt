package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.error.TkResult
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/** Why a formula couldn't be read */
class FormulaError(override val message: String) : Error

/**
 * A formula as A2L's FORM conversions and virtual characteristics write it: C style arithmetic, comparison, logic and
 * bit operators (`^` is xor, as in C; `pow(x, y)` is the power), the math functions ASAM lists, `X` or `X1`..`Xn` for
 * the inputs, and `sysc(NAME)` for a MOD_PAR SYSTEM_CONSTANT, which is filled in when the formula is read.
 * Bit operators work on the values as whole numbers.
 *
 * The text is parsed into a small tree of operators and evaluated by walking it: nothing in it is ever run as code,
 * and only the operators and functions listed here exist. Formulas are capped at [MAX_NODES] parts so a huge one
 * can't make evaluation (which recurses through the tree) overflow the stack
 */
class Expression private constructor(val text: String, private val root: Node, val inputCount: Int) {
    /** The value for these inputs ([inputs] first is X1). Missing inputs are 0; a bad operation gives NaN or infinity */
    fun evaluate(vararg inputs: Double): Double = root.eval(inputs)

    override fun toString(): String = text

    companion object {
        /** Real formulas have a few dozen parts at most */
        const val MAX_NODES = 2000

        /**
         * @param constants SYSTEM_CONSTANTs by name, for `sysc()`
         */
        fun parse(text: String, constants: Map<String, String> = emptyMap()): TkResult<Expression, FormulaError> = try {
            val parser = ExpressionParser(text, constants)
            val root = parser.parse()
            TkResult.Success(Expression(text, root, parser.inputCount))
        } catch (e: ExpressionSyntaxError) {
            TkResult.Failure(FormulaError("Formula \"$text\": ${e.message}"))
        }
    }
}

/** One part of a parsed formula; [eval] walks the tree, it doesn't run anything */
private sealed interface Node {
    fun eval(inputs: DoubleArray): Double

    class Number(val value: Double) : Node {
        override fun eval(inputs: DoubleArray) = value
    }

    class Input(val index: Int) : Node {
        override fun eval(inputs: DoubleArray) = inputs.getOrElse(index) { 0.0 }
    }

    class Unary(val op: String, val operand: Node) : Node {
        override fun eval(inputs: DoubleArray): Double {
            val x = operand.eval(inputs)
            return when (op) {
                "-" -> -x
                "+" -> x
                "!" -> if (x == 0.0) 1.0 else 0.0
                else -> x.toLong().inv().toDouble() // ~
            }
        }
    }

    class Binary(val op: String, val left: Node, val right: Node) : Node {
        override fun eval(inputs: DoubleArray): Double {
            val a = left.eval(inputs)
            // && and || only look at the right side when they need to, as in C
            if (op == "&&") return if (a != 0.0 && right.eval(inputs) != 0.0) 1.0 else 0.0
            if (op == "||") return if (a != 0.0 || right.eval(inputs) != 0.0) 1.0 else 0.0
            val b = right.eval(inputs)
            return when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                "%" -> a % b
                "<" -> (a < b).toDouble()
                ">" -> (a > b).toDouble()
                "<=" -> (a <= b).toDouble()
                ">=" -> (a >= b).toDouble()
                "==" -> (a == b).toDouble()
                "!=" -> (a != b).toDouble()
                "&" -> (a.toLong() and b.toLong()).toDouble()
                "|" -> (a.toLong() or b.toLong()).toDouble()
                "^" -> (a.toLong() xor b.toLong()).toDouble()
                "<<" -> (a.toLong() shl b.toInt()).toDouble()
                else -> (a.toLong() shr b.toInt()).toDouble() // >>
            }
        }
    }

    class Call(val function: String, val arguments: List<Node>) : Node {
        override fun eval(inputs: DoubleArray): Double {
            val x = arguments[0].eval(inputs)
            return when (function) {
                "abs" -> abs(x)
                "sqrt" -> sqrt(x)
                "exp" -> exp(x)
                "log" -> ln(x)
                "log10" -> log10(x)
                "sin" -> sin(x)
                "cos" -> cos(x)
                "tan" -> tan(x)
                "asin", "arcsin" -> asin(x)
                "acos", "arccos" -> acos(x)
                "atan", "arctan" -> atan(x)
                "sinh" -> sinh(x)
                "cosh" -> cosh(x)
                "tanh" -> tanh(x)
                "ceil" -> ceil(x)
                "floor" -> floor(x)
                "pow" -> x.pow(arguments[1].eval(inputs))
                "min" -> minOf(x, arguments[1].eval(inputs))
                "max" -> maxOf(x, arguments[1].eval(inputs))
                else -> Double.NaN
            }
        }
    }
}

private fun Boolean.toDouble() = if (this) 1.0 else 0.0

private class ExpressionSyntaxError(message: String) : Exception(message)

/** Precedence climbing over C's operator levels, lowest first */
private class ExpressionParser(private val text: String, private val constants: Map<String, String>) {
    private var position = 0
    private var depth = 0
    private var nodes = 0
    var inputCount = 0
        private set

    fun parse(): Node {
        val node = binary(0)
        skipSpace()
        if (position < text.length) throw ExpressionSyntaxError("unexpected '${text[position]}' at ${position + 1}")
        return node
    }

    private fun binary(level: Int): Node {
        if (level == LEVELS.size) return unary()
        var left = binary(level + 1)
        while (true) {
            val op = LEVELS[level].firstOrNull { matchesOperator(it) } ?: return left
            position += op.length
            count()
            left = Node.Binary(op, left, binary(level + 1))
        }
    }

    /** [op] is next, and isn't the start of a longer operator (`<` of `<<`, `&` of `&&`) */
    private fun matchesOperator(op: String): Boolean {
        skipSpace()
        if (!text.startsWith(op, position)) return false
        val longer = LONGER[op] ?: return true
        return longer.none { text.startsWith(it, position) }
    }

    private fun unary(): Node {
        skipSpace()
        val char = text.getOrNull(position)
        if (char == '-' || char == '+' || char == '!' || char == '~') {
            position++
            count()
            return nested { Node.Unary(char.toString(), unary()) }
        }
        return primary()
    }

    private fun primary(): Node {
        skipSpace()
        val char = text.getOrNull(position) ?: throw ExpressionSyntaxError("it ends where a value was expected")
        return when {
            char == '(' -> {
                position++
                val inner = nested { binary(0) }
                expect(')')
                inner
            }
            char.isDigit() || char == '.' -> Node.Number(number())
            char.isLetter() || char == '_' -> name()
            else -> throw ExpressionSyntaxError("unexpected '$char' at ${position + 1}")
        }
    }

    private fun name(): Node {
        val start = position
        while (position < text.length && (text[position].isLetterOrDigit() || text[position] == '_')) position++
        val name = text.substring(start, position)
        val lower = name.lowercase()
        skipSpace()
        if (text.getOrNull(position) == '(') {
            position++
            if (lower == "sysc") return systemConstant()
            val arity = FUNCTIONS[lower] ?: throw ExpressionSyntaxError("unknown function $name")
            val arguments = mutableListOf(nested { binary(0) })
            while (arguments.size < arity) {
                expect(',')
                arguments += nested { binary(0) }
            }
            expect(')')
            return Node.Call(lower, arguments)
        }
        if (lower == "x") return input(1)
        val index = if (lower.startsWith("x")) lower.drop(1).toIntOrNull() else null
        if (index != null && index >= 1) return input(index)
        throw ExpressionSyntaxError("unknown name $name")
    }

    private fun input(index: Int): Node {
        inputCount = maxOf(inputCount, index)
        return Node.Input(index - 1)
    }

    /** `sysc(NAME)` or `sysc("NAME")`: the constant's value, now */
    private fun systemConstant(): Node {
        skipSpace()
        val quoted = text.getOrNull(position) == '"'
        if (quoted) position++
        val start = position
        while (position < text.length && text[position] != ')' && text[position] != '"') position++
        val name = text.substring(start, position).trim()
        if (quoted) expect('"')
        expect(')')
        val value = constants[name] ?: throw ExpressionSyntaxError("there's no SYSTEM_CONSTANT $name")
        return Node.Number(value.trim().toDoubleOrNull() ?: throw ExpressionSyntaxError("SYSTEM_CONSTANT $name isn't a number"))
    }

    private fun number(): Double {
        val start = position
        if (text.startsWith("0x", position, ignoreCase = true)) {
            position += 2
            while (position < text.length && text[position].isLetterOrDigit()) position++
            return text.substring(start + 2, position).toLongOrNull(16)?.toDouble()
                ?: throw ExpressionSyntaxError("bad hex number ${text.substring(start, position)}")
        }
        while (position < text.length && (text[position].isDigit() || text[position] == '.')) position++
        if (position < text.length && (text[position] == 'e' || text[position] == 'E')) {
            val save = position
            position++
            if (position < text.length && (text[position] == '+' || text[position] == '-')) position++
            if (position < text.length && text[position].isDigit()) {
                while (position < text.length && text[position].isDigit()) position++
            } else {
                position = save
            }
        }
        return text.substring(start, position).toDoubleOrNull() ?: throw ExpressionSyntaxError("bad number ${text.substring(start, position)}")
    }

    private fun expect(char: Char) {
        skipSpace()
        if (text.getOrNull(position) != char) {
            throw ExpressionSyntaxError(text.getOrNull(position)?.let { "expected '$char' at ${position + 1}, found '$it'" } ?: "it ends where '$char' was expected")
        }
        position++
    }

    private fun skipSpace() {
        while (position < text.length && text[position].isWhitespace()) position++
    }

    private fun count() {
        if (++nodes > Expression.MAX_NODES) throw ExpressionSyntaxError("it has more than ${Expression.MAX_NODES} operators")
    }

    /** Keeps a formula nested absurdly deep from overflowing the stack */
    private inline fun <T> nested(block: () -> T): T {
        if (++depth > MAX_NESTING) throw ExpressionSyntaxError("it's nested more than $MAX_NESTING deep")
        try {
            return block()
        } finally {
            depth--
        }
    }

    companion object {
        const val MAX_NESTING = 200

        val LEVELS = listOf(
            listOf("||"), listOf("&&"), listOf("|"), listOf("^"), listOf("&"),
            listOf("==", "!="), listOf("<=", ">=", "<", ">"), listOf("<<", ">>"), listOf("+", "-"), listOf("*", "/", "%")
        )

        /** Operators that start with a shorter one */
        val LONGER = mapOf("|" to listOf("||"), "&" to listOf("&&"), "<" to listOf("<<", "<="), ">" to listOf(">>", ">="))

        /** Functions and how many arguments they take */
        val FUNCTIONS = mapOf(
            "abs" to 1, "sqrt" to 1, "exp" to 1, "log" to 1, "log10" to 1,
            "sin" to 1, "cos" to 1, "tan" to 1, "asin" to 1, "acos" to 1, "atan" to 1, "arcsin" to 1, "arccos" to 1, "arctan" to 1,
            "sinh" to 1, "cosh" to 1, "tanh" to 1, "ceil" to 1, "floor" to 1, "pow" to 2, "min" to 2, "max" to 2
        )
    }
}
