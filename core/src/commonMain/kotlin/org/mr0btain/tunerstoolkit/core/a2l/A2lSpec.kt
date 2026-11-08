package org.mr0btain.tunerstoolkit.core.a2l

/**
 * What one A2L keyword takes. A2L can't be parsed without this: a keyword and a name look the same, so the parser
 * has to know how many parameters each keyword has to tell where the next keyword starts.
 *
 * Parameters are written as a pattern of space separated parts:
 * - `I` a name, `S` a string, `N` a number, `A` any one value
 * - `N*` numbers for as long as there are numbers, `S*` strings likewise
 * - `I*` names up to the next keyword this table knows (or `/begin`, `/end`)
 * - `*` every value up to `/end`, keywords or not (for lists of names)
 * - `(NS)*` the group in brackets, for as long as the next value is a number (COMPU_VTAB's pairs, say)
 * - `?` after a part: it may be missing
 *
 * @property block `/begin KEYWORD ... /end KEYWORD` rather than a keyword and its parameters
 * @property raw The block's content isn't A2L (A2ML) or is defined by A2ML (IF_DATA): it's kept as source text
 */
class A2lRule(val keyword: String, pattern: String, val block: Boolean, val raw: Boolean = false) {
    val params: List<ParamPart> = ParamPart.parse(pattern)
}

/** One part of an [A2lRule]'s pattern */
sealed interface ParamPart {
    val optional: Boolean

    data class One(val kind: ValueKind, override val optional: Boolean = false) : ParamPart
    data class Many(val kind: ValueKind, override val optional: Boolean = true) : ParamPart
    data class NamesToKeyword(override val optional: Boolean = true) : ParamPart
    data class AllToEnd(override val optional: Boolean = true) : ParamPart
    data class Group(val kinds: List<ValueKind>, override val optional: Boolean = true) : ParamPart

    companion object {
        fun parse(pattern: String): List<ParamPart> = pattern.split(' ').filter { it.isNotEmpty() }.map { part ->
            val optional = part.endsWith("?")
            val body = part.removeSuffix("?")
            when {
                body == "*" -> AllToEnd()
                body == "I*" -> NamesToKeyword()
                body.startsWith("(") && body.endsWith(")*") -> Group(body.substring(1, body.length - 2).map(ValueKind::of))
                body.length == 2 && body[1] == '*' -> Many(ValueKind.of(body[0]))
                body.length == 1 -> One(ValueKind.of(body[0]), optional)
                else -> error("Bad A2L pattern part $part")
            }
        }
    }
}

enum class ValueKind {
    IDENT, STRING, NUMBER, ANY;

    companion object {
        fun of(char: Char): ValueKind = when (char) {
            'I' -> IDENT
            'S' -> STRING
            'N' -> NUMBER
            'A' -> ANY
            else -> error("Bad A2L pattern kind $char")
        }
    }
}

/**
 * Every keyword of ASAM MCD-2 MC 1.5.1 to 1.7.1 the parser knows. Keywords that aren't here are still read (as names
 * followed by values) and kept, with a warning
 */
object A2lSpec {
    private val axes = listOf("X", "Y", "Z", "4", "5")

    val rules: Map<String, A2lRule> = buildList {
        fun block(keyword: String, pattern: String = "") = add(A2lRule(keyword, pattern, block = true))
        fun simple(keyword: String, pattern: String = "") = add(A2lRule(keyword, pattern, block = false))

        // ---------------------------------------- Structure
        simple("ASAP2_VERSION", "N N")
        simple("A2ML_VERSION", "N N")
        block("PROJECT", "I S")
        block("HEADER", "S")
        simple("VERSION", "S")
        simple("PROJECT_NO", "I")
        block("MODULE", "I S")
        add(A2lRule("A2ML", "", block = true, raw = true))
        add(A2lRule("IF_DATA", "", block = true, raw = true))

        // ---------------------------------------- MOD_PAR
        block("MOD_PAR", "S")
        simple("ADDR_EPK", "N")
        simple("EPK", "S")
        simple("SUPPLIER", "S")
        simple("CUSTOMER", "S")
        simple("CUSTOMER_NO", "S")
        simple("USER", "S")
        simple("PHONE_NO", "S")
        simple("ECU", "S")
        simple("CPU_TYPE", "S")
        simple("NO_OF_INTERFACES", "N")
        simple("ECU_CALIBRATION_OFFSET", "N")
        block("CALIBRATION_METHOD", "S N")
        block("CALIBRATION_HANDLE", "N*")
        simple("CALIBRATION_HANDLE_TEXT", "S")
        block("MEMORY_LAYOUT", "I N N N N N N N")
        block("MEMORY_SEGMENT", "I S I I I N N N N N N N")
        simple("SYSTEM_CONSTANT", "S S")

        // ---------------------------------------- MOD_COMMON
        block("MOD_COMMON", "S")
        simple("S_REC_LAYOUT", "I")
        simple("DEPOSIT", "I")
        simple("BYTE_ORDER", "I")
        simple("DATA_SIZE", "N")
        for (alignment in listOf("BYTE", "WORD", "LONG", "INT64", "FLOAT16_IEEE", "FLOAT32_IEEE", "FLOAT64_IEEE")) {
            simple("ALIGNMENT_$alignment", "N")
        }

        // ---------------------------------------- Objects
        block("CHARACTERISTIC", "I S I N I N I N N")
        block("AXIS_PTS", "I S N I I N I N N N")
        block("MEASUREMENT", "I S I I N N N N")
        block("BLOB", "I S N N")
        block("INSTANCE", "I S I N")
        block("TYPEDEF_CHARACTERISTIC", "I S I I N I N N")
        block("TYPEDEF_AXIS", "I S I I N I N N N")
        block("TYPEDEF_MEASUREMENT", "I S I I N N N N")
        block("TYPEDEF_STRUCTURE", "I S N")
        block("TYPEDEF_BLOB", "I S N")
        block("STRUCTURE_COMPONENT", "I I N")
        block("TRANSFORMER", "I S S S N I I")
        block("TRANSFORMER_IN_OBJECTS", "*")
        block("TRANSFORMER_OUT_OBJECTS", "*")
        block("AXIS_DESCR", "I I I N N N")

        // ---------------------------------------- Axes
        simple("AXIS_PTS_REF", "I")
        simple("CURVE_AXIS_REF", "I")
        simple("FIX_AXIS_PAR", "N N N")
        simple("FIX_AXIS_PAR_DIST", "N N N")
        block("FIX_AXIS_PAR_LIST", "N*")
        simple("MAX_GRAD", "N")

        // ---------------------------------------- RECORD_LAYOUT
        block("RECORD_LAYOUT", "I")
        simple("FNC_VALUES", "N I I I")
        simple("IDENTIFICATION", "N I")
        for (axis in axes) {
            simple("AXIS_PTS_$axis", "N I I I")
            simple("AXIS_RESCALE_$axis", "N I N I I")
            simple("NO_AXIS_PTS_$axis", "N I")
            simple("NO_RESCALE_$axis", "N I")
            simple("FIX_NO_AXIS_PTS_$axis", "N")
            simple("SRC_ADDR_$axis", "N I")
            simple("RIP_ADDR_$axis", "N I")
            simple("SHIFT_OP_$axis", "N I")
            simple("OFFSET_$axis", "N I")
            simple("DIST_OP_$axis", "N I")
        }
        simple("RIP_ADDR_W", "N I")
        simple("RESERVED", "N I")
        simple("STATIC_RECORD_LAYOUT")
        simple("STATIC_ADDRESS_OFFSETS")

        // ---------------------------------------- Conversion
        block("COMPU_METHOD", "I S I S S")
        simple("COEFFS", "N N N N N N")
        simple("COEFFS_LINEAR", "N N")
        simple("COMPU_TAB_REF", "I")
        block("FORMULA", "S")
        simple("FORMULA_INV", "S")
        simple("REF_UNIT", "I")
        simple("STATUS_STRING_REF", "I")
        block("COMPU_TAB", "I S I N (NN)*")
        block("COMPU_VTAB", "I S I N (NS)*")
        block("COMPU_VTAB_RANGE", "I S N (NNS)*")
        simple("DEFAULT_VALUE", "S")
        simple("DEFAULT_VALUE_NUMERIC", "N")
        block("UNIT", "I S S I")
        simple("SI_EXPONENTS", "N N N N N N N")
        simple("UNIT_CONVERSION", "N N")

        // ---------------------------------------- Organisation
        block("FUNCTION", "I S")
        simple("FUNCTION_VERSION", "S")
        block("DEF_CHARACTERISTIC", "*")
        block("REF_CHARACTERISTIC", "*")
        block("IN_MEASUREMENT", "*")
        block("OUT_MEASUREMENT", "*")
        block("LOC_MEASUREMENT", "*")
        block("SUB_FUNCTION", "*")
        block("FUNCTION_LIST", "*")
        block("AR_COMPONENT", "S")
        simple("AR_PROTOTYPE_OF", "I")
        block("GROUP", "I S")
        simple("ROOT")
        block("REF_MEASUREMENT", "*")
        block("SUB_GROUP", "*")
        block("FRAME", "I S N N")
        simple("FRAME_MEASUREMENT", "I*")
        block("USER_RIGHTS", "I")
        block("REF_GROUP", "*")
        block("VARIANT_CODING")
        simple("VAR_SEPARATOR", "S")
        simple("VAR_NAMING", "I")
        block("VAR_CRITERION", "I S I*")
        simple("VAR_MEASUREMENT", "I")
        simple("VAR_SELECTION_CHARACTERISTIC", "I")
        block("VAR_FORBIDDEN_COMB", "*")
        block("VAR_CHARACTERISTIC", "I I*")
        block("VAR_ADDRESS", "N*")

        // ---------------------------------------- Options on objects
        simple("BIT_MASK", "N")
        block("BIT_OPERATION")
        simple("LEFT_SHIFT", "N")
        simple("RIGHT_SHIFT", "N")
        simple("SIGN_EXTEND")
        simple("MATRIX_DIM", "N*")
        simple("NUMBER", "N")
        simple("EXTENDED_LIMITS", "N N")
        simple("READ_ONLY")
        simple("READ_WRITE")
        simple("GUARD_RAILS")
        simple("DISCRETE")
        simple("DISPLAY_IDENTIFIER", "I")
        simple("FORMAT", "S")
        simple("PHYS_UNIT", "S")
        simple("MAX_REFRESH", "N N")
        simple("COMPARISON_QUANTITY", "I")
        block("DEPENDENT_CHARACTERISTIC", "S I*")
        block("VIRTUAL_CHARACTERISTIC", "S I*")
        block("VIRTUAL", "*")
        simple("SYMBOL_LINK", "S N")
        simple("MODEL_LINK", "S")
        simple("ENCODING", "I")
        simple("CALIBRATION_ACCESS", "I")
        simple("MONOTONY", "I")
        simple("STEP_SIZE", "N")
        simple("ECU_ADDRESS", "N")
        simple("ECU_ADDRESS_EXTENSION", "N")
        simple("ERROR_MASK", "N")
        simple("LAYOUT", "I")
        simple("ARRAY_SIZE", "N")
        simple("ADDRESS_TYPE", "I")
        simple("REF_MEMORY_SEGMENT", "I")
        simple("CONSISTENT_EXCHANGE")
        simple("SYMBOL_TYPE_LINK", "S")
        block("MAP_LIST", "*")
        block("ANNOTATION")
        simple("ANNOTATION_LABEL", "S")
        simple("ANNOTATION_ORIGIN", "S")
        block("ANNOTATION_TEXT", "S*")
        // INSTANCE's OVERWRITE (1.7)
        block("OVERWRITE", "I N")
        simple("INPUT_QUANTITY", "I")
        simple("CONVERSION", "I")
        simple("LIMITS", "N N")
    }.associateBy { it.keyword }

    fun rule(keyword: String): A2lRule? = rules[keyword]
}
