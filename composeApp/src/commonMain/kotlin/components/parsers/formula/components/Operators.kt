package components.parsers.formula.components

enum class Operators(val sign: Char) {
    FACTORIAL('!'),
    MINUS('-'),
    PLUS('+'),
    MULTIPLY('*'),
    DIVISION('/'),
    POWER('^'),
    EXPONENTIAL('E'),
    OPEN_BRACKET('('),
    END_BRACKET(')');
}