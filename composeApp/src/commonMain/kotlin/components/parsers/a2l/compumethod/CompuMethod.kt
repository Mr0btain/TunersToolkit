package components.parsers.a2l.compumethod

import components.parsers.a2l.computab.CompuTab
import components.parsers.a2l.compuvtab.CompuVTab
import components.parsers.a2l.compuvtabrange.CompuVTabRange
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.data.Coeffs
import components.parsers.a2l.classes.data.CoeffsLinear
import components.parsers.a2l.classes.data.CompuTabRef
import components.parsers.a2l.classes.data.Formula
import components.parsers.a2l.classes.data.RefUnit
import components.parsers.a2l.classes.data.StatusStringRef
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.computab.applyTabIntP
import components.parsers.a2l.computab.applyTabNoIntP
import components.parsers.a2l.objects.A2lOptional
import components.calibrationdata.a2l.helpers.applyRatFunc
import kotlinx.serialization.Serializable


/**
 * Specification of a conversion method used to convert values from the data file
 * to the actual data used by the ecu
 */

@Serializable
data class CompuMethod(
    val name: String = "",
    val longIdentifier: String = "",
    val conversionType: ConversionType = ConversionType.NOT_SET,
    val format: String = "",
    val unit: String = "",
    val optionalParameters: Map<CompuMethodOpt, A2lOptional> = mapOf()
)

fun CompuMethod.validate(): Boolean {
    return when {
        name.isBlank() -> false
        conversionType == ConversionType.NOT_SET -> false
        else -> true
    }
}

// TODO: Redo This when functions added
/**
 * Compute the value
 */
fun CompuMethod.compute(
    input: Number,
    a2lData: A2lData
): Double {
    val mValue = input.toLong()

    return when (conversionType) {
        ConversionType.IDENTICAL -> {
            mValue.toDouble()
        }

        ConversionType.FORM -> {
            // TODO: Figure out how to parse a formula
            mValue.toDouble()
        }

        ConversionType.LINEAR -> {
            getCoeffsLinear().compute(input.toDouble())
        }

        ConversionType.RAT_FUNC -> {
            // Compute
            mValue.applyRatFunc(getCoeffs())
        }

        ConversionType.TAB_NOINTP -> {
            // TODO: Test
            getCompuTabRef(a2lData).applyTabNoIntP(mValue)
        }

        ConversionType.TAB_INTP -> {
            // TODO: Test
            getCompuTabRef(a2lData).applyTabIntP(mValue)
        }

        ConversionType.TAB_VERB -> mValue.toDouble()
        ConversionType.NOT_SET -> mValue.toDouble()
    }
}

/**
 * Compute the value Tab verb
 */

fun CompuMethod.computeTabVerb(input: Number, a2lData: A2lData): String {
    val num = input.toInt()
    val compuVTabRef = getCompuVTab(a2lData)
    val compuVTabRangeRef = getCompuVTabRange(a2lData)
    var result = ""

    if (compuVTabRef.valuePairs.containsKey(num)) {
        result = compuVTabRef.valuePairs[num] ?: ""
    } else if (compuVTabRangeRef.valueTriples.isNotEmpty()) {
        compuVTabRangeRef.valueTriples.forEach { value ->
            result = value.takeIf { (it.inValMin <= num && it.inValMax >= num) }?.outVal ?: ""
        }
    }

    return result
}

/**
 * Checks and Returns the Reference CompuTab if set
 */
private fun CompuMethod.getCompuTabRef(a2lData: A2lData): CompuTab {
    return if (optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
        val mCompuTab = optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

        if (mCompuTab.reference.isNotBlank() && a2lData.compuTabs.containsKey(mCompuTab.reference)) {
            return a2lData.compuTabs[mCompuTab.reference] ?: CompuTab()
        } else CompuTab()
    } else CompuTab()
}

/**
 * Checks and Returns the Reference CompuVTab if it exists
 */
fun CompuMethod.getCompuVTab(a2lData: A2lData): CompuVTab {
    return if (optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
        val mCompuVTab = optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

        if (mCompuVTab.reference.isNotBlank() && a2lData.compuVTabs.containsKey(mCompuVTab.reference)) {
            return a2lData.compuVTabs[mCompuVTab.reference] ?: CompuVTab()
        } else CompuVTab()
    } else CompuVTab()
}

/**
 * Checks and Returns the Reference CompuVTabRange if it exists
 */
fun CompuMethod.getCompuVTabRange(a2lData: A2lData): CompuVTabRange {
    return if (optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
        val mCompuTab = optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

        if (mCompuTab.reference.isNotBlank() && a2lData.compuVTabRanges.containsKey(mCompuTab.reference)) {
            return a2lData.compuVTabRanges[mCompuTab.reference] ?: CompuVTabRange()
        } else CompuVTabRange()
    } else CompuVTabRange()
}

/**
 * Checks and Returns the Coeffs if set
 */
fun CompuMethod.getCoeffs(): Coeffs {
    return if (optionalParameters.containsKey(CompuMethodOpt.Coeffs)) {
        optionalParameters[CompuMethodOpt.Coeffs] as? Coeffs ?: Coeffs()
    } else Coeffs()
}

/**
 * Gets and Returns the Coeffs Linear if set
 */
fun CompuMethod.getCoeffsLinear(): CoeffsLinear {
    return if (optionalParameters.containsKey(CompuMethodOpt.CoeffsLinear)) {
        optionalParameters[CompuMethodOpt.CoeffsLinear] as? CoeffsLinear ?: CoeffsLinear()
    } else CoeffsLinear()
}

/**
 * Checks and Returns the Formula if set
 */
fun CompuMethod.getFormula(): String {
    return if (optionalParameters.containsKey(CompuMethodOpt.Formula)) {
        val res = optionalParameters[CompuMethodOpt.Formula] as? Formula ?: Formula()
        res.reference
    } else ""
}

/**
 * Checks and Returns the Reference Unit if set
 */
fun CompuMethod.getRefUnit(): String {
    return if (optionalParameters.containsKey(CompuMethodOpt.RefUnit)) {
        val res = optionalParameters[CompuMethodOpt.RefUnit] as? RefUnit ?: RefUnit()
        res.ref
    } else ""
}

/**
 * Checks and Returns the Status String Reference if set
 */
fun CompuMethod.getStatusStringRef(): String {
    return if (optionalParameters.containsKey(CompuMethodOpt.StatusStringRef)) {
        val res = optionalParameters[CompuMethodOpt.StatusStringRef] as? StatusStringRef ?: StatusStringRef()
        res.reference
    } else ""
}