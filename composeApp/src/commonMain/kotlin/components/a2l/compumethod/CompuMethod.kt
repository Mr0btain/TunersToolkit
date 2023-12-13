package components.a2l.compumethod

import components.a2l.computab.CompuTab
import components.a2l.compuvtab.CompuVTab
import components.a2l.compuvtabrange.CompuVTabRange
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.Coeffs
import components.a2l.classes.data.CoeffsLinear
import components.a2l.classes.data.CompuTabRef
import components.a2l.classes.data.Formula
import components.a2l.classes.data.RefUnit
import components.a2l.classes.data.StatusStringRef
import components.a2l.classes.enums.ConversionType
import components.a2l.objects.A2lOptional
import components.calibrationdata.helpers.applyRatFunc
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
) {
    fun isNotEmpty(): Boolean {
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
    fun compute(
        input: Number,
        a2lData: A2lData,
        format: String
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
                this.getCoeffsLinear().compute(input.toDouble())
            }

            ConversionType.RAT_FUNC -> {
                // Compute
                mValue.applyRatFunc(this.getCoeffs())
            }

            ConversionType.TAB_NOINTP -> {
                // TODO: Test
                this.getCompuTabRef(a2lData).applyTabNoIntP(mValue)
            }

            ConversionType.TAB_INTP -> {
                // TODO: Test
                this.getCompuTabRef(a2lData).applyTabIntP(mValue)
            }

            ConversionType.TAB_VERB -> mValue.toDouble()
            ConversionType.NOT_SET -> mValue.toDouble()
        }
    }

    /**
     * Compute the value Tab verb
     */

    fun computeTabVerb(input: Number, a2lData: A2lData): String {
        val num = input.toInt()
        val compuVTabRef = this.getCompuVTab(a2lData)
        val compuVTabRangeRef = this.getCompuVTabRange(a2lData)
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
    private fun getCompuTabRef(a2lData: A2lData): CompuTab {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
            val mCompuTab = this.optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

            if (mCompuTab.reference.isNotBlank() && a2lData.compuTabs.containsKey(mCompuTab.reference)) {
                return a2lData.compuTabs[mCompuTab.reference] ?: CompuTab()
            } else CompuTab()
        } else CompuTab()
    }

    /**
     * Checks and Returns the Reference CompuVTab if it exists
     */
    fun getCompuVTab(a2lData: A2lData): CompuVTab {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
            val mCompuVTab = this.optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

            if (mCompuVTab.reference.isNotBlank() && a2lData.compuVTabs.containsKey(mCompuVTab.reference)) {
                return a2lData.compuVTabs[mCompuVTab.reference] ?: CompuVTab()
            } else CompuVTab()
        } else CompuVTab()
    }

    /**
     * Checks and Returns the Reference CompuVTabRange if it exists
     */
    fun getCompuVTabRange(a2lData: A2lData): CompuVTabRange {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.CompuTabRef)) {
            val mCompuTab = this.optionalParameters[CompuMethodOpt.CompuTabRef] as? CompuTabRef ?: CompuTabRef()

            if (mCompuTab.reference.isNotBlank() && a2lData.compuVTabRanges.containsKey(mCompuTab.reference)) {
                return a2lData.compuVTabRanges[mCompuTab.reference] ?: CompuVTabRange()
            } else CompuVTabRange()
        } else CompuVTabRange()
    }

    /**
     * Checks and Returns the Coeffs if set
     */
    fun getCoeffs(): Coeffs {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.Coeffs)) {
            this.optionalParameters[CompuMethodOpt.Coeffs] as? Coeffs ?: Coeffs()
        } else Coeffs()
    }

    /**
     * Gets and Returns the Coeffs Linear if set
     */
    fun getCoeffsLinear(): CoeffsLinear {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.CoeffsLinear)) {
            this.optionalParameters[CompuMethodOpt.CoeffsLinear] as? CoeffsLinear ?: CoeffsLinear()
        } else CoeffsLinear()
    }

    /**
     * Checks and Returns the Formula if set
     */
    fun getFormula(): String {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.Formula)) {
            val res = this.optionalParameters[CompuMethodOpt.Formula] as? Formula ?: Formula()
            res.reference
        } else ""
    }

    /**
     * Checks and Returns the Reference Unit if set
     */
    fun getRefUnit(): String {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.RefUnit)) {
            val res = this.optionalParameters[CompuMethodOpt.RefUnit] as? RefUnit ?: RefUnit()
            res.ref
        } else ""
    }

    /**
     * Checks and Returns the Status String Reference if set
     */
    fun getStatusStringRef(): String {
        return if (this.optionalParameters.containsKey(CompuMethodOpt.StatusStringRef)) {
            val res = this.optionalParameters[CompuMethodOpt.StatusStringRef] as? StatusStringRef ?: StatusStringRef()
            res.reference
        } else ""
    }
}