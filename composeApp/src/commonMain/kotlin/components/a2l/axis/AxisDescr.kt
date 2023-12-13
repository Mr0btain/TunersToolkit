@file:Suppress("ArrayInDataClass", "unused")

package components.a2l.axis

import components.a2l.characteristic.Characteristic
import components.a2l.compumethod.CompuMethod
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.AxisPointReference
import components.a2l.classes.data.CurveReferenceAxis
import components.a2l.classes.data.ExtendedLimits
import components.a2l.classes.data.FixAxisPar
import components.a2l.classes.data.FixAxisParDist
import components.a2l.classes.data.FixAxisParList
import components.a2l.classes.data.Format
import components.a2l.classes.data.MaxGradient
import components.a2l.classes.data.PhysUnit
import components.a2l.classes.data.StepSize
import components.a2l.classes.enums.AxisType
import components.a2l.classes.enums.Deposit
import components.a2l.classes.enums.Monotony
import components.helpers.checkItemsAre
import components.a2l.classes.enums.ByteOrder
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Description of an Axis in a Map
 * @param type Description of the axis points: STD_AXIS, COM_AXIS, FIX_AXIS, CURVE_AXIS,
 * RES_AXIS, NOT_SET. If NOT_SET Then there has been an error parsing the data
 * @param inputQuantity Reference to the data record for description of the input quantity
 * (see MEASUREMENT). If there is no input quantity assigned, parameter 'InputQuantity'
 * should be set to "NO_INPUT_QUANTITY" (measurement and calibration systems must be
 * capable to treat this case).
 * @param conversion Reference to the relevant record of the description of
 * the conversion method (see COMPU_METHOD). If there is no conversion method, as in the case of
 * CURVE_AXIS, the parameter ‗Conversion should be set to ―NO_COMPU_METHOD
 * (measurement and calibration systems must be able to handle this case).
 * @param maxAxisPoints Maximum number of axis points
 * Note: The measurement and calibration system can change the dimensions of a characteristic
 * (increase or decrease the number of axis points). The number of axis points may not be
 * increased at random as the address range reserved for each characteristic in the ECU
 * program by the measurement and calibration system cannot be changed.
 * @param lowerLimit Plausible range of axis point values, lower limit
 * @param upperLimit Plausible range of axis point values, upper limit
 */

@Serializable
data class AxisDescr(
    val type: AxisType = AxisType.NOT_SET,
    val inputQuantity: String = "",
    val conversion: String = "",
    val maxAxisPoints: UInt = UInt.MIN_VALUE,
    val lowerLimit: Double = Double.NaN,
    val upperLimit: Double = Double.NaN,
    val optionalParameters: Map<AxisDescrOpt, A2lOptional> = mapOf()
): A2lOptional {
    fun isNotEmpty(): Boolean {
        return when {
            type == AxisType.NOT_SET -> false
            else -> true
        }
    }

    /**
     * Checks and Returns the Format if set
     */
    fun getFormat(): String {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.Format)) {
            val res = this.optionalParameters[AxisDescrOpt.Format] as? Format ?: Format()
            res.value
        } else ""
    }

    /**
     * Checks and Returns the Reference Axis Point if set
     */
    fun getRefAxisPoint(a2lData: A2lData): AxisPts {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.ReferenceAxisPoint)) {
            val result = this.optionalParameters[AxisDescrOpt.ReferenceAxisPoint] as? AxisPointReference ?: AxisPointReference()

            if (result != AxisPointReference() && a2lData.axisPoints.containsKey(result.name)) {
                return a2lData.axisPoints[result.name] ?: AxisPts()
            } else AxisPts()
        } else AxisPts()
    }

    /**
     * Returns True if Read Only is set
     */
    fun isReadOnly(): Boolean {
        return this.optionalParameters.containsKey(AxisDescrOpt.ReadOnly)
    }

    /**
     * Checks and Returns the Maximum Gradient Allowed if set
     */
    fun getMaxGradient(): Double {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.MaxGradient)) {
            val res = this.optionalParameters[AxisDescrOpt.MaxGradient] as? MaxGradient ?: MaxGradient()
            res.value
        } else Double.NaN
    }

    /**
     * Checks and Returns the Monotony if set
     */
    fun getMonotony(): Monotony {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.Monotony)) {
            this.optionalParameters[AxisDescrOpt.Monotony] as? Monotony ?: Monotony.NOT_SET
        } else Monotony.NOT_SET
    }

    /**
     * Checks and Returns the Byte order if set
     */
    fun getByteOrder(): ByteOrder {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.ByteOrder)) {
            this.optionalParameters[AxisDescrOpt.Monotony] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
        } else ByteOrder.LITTLE_ENDIAN
    }

    /**
     * Checks and returns the compu method for conversion
     */
    fun getCompuMethod(a2lData: A2lData): CompuMethod {
        return if (a2lData.compuMethods.containsKey(this.conversion)) {
            a2lData.compuMethods[this.conversion] ?: CompuMethod()
        } else CompuMethod()
    }

    /**
     * Checks and Returns the Extended Limits if set
     */
    fun getExtendedLimits(): ExtendedLimits {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.ExtendedLimits)) {
            this.optionalParameters[AxisDescrOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
        } else ExtendedLimits()
    }

    /**
     * Checks and Returns the Reference Curve Axis if set
     */
    fun getReferenceCurveAxis(a2lData: A2lData): Characteristic {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.ReferenceCurveAxis)) {
            val mRefCurve = this.optionalParameters[AxisDescrOpt.ReferenceCurveAxis] as? CurveReferenceAxis ?: CurveReferenceAxis()

            if (mRefCurve.reference.isNotBlank() && a2lData.characteristics.containsKey(mRefCurve.reference)) {
                return a2lData.characteristics[mRefCurve.reference] ?: Characteristic()
            } else Characteristic()
        } else Characteristic()
    }

    /**
     * Checks and Returns the Deposit of the values in Memory if set
     */
    fun getDeposit(): Deposit {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.Deposit)) {
            this.optionalParameters[AxisDescrOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
        } else Deposit.NOT_SET
    }

    /**
     * Checks and Returns the Phys Unit if set
     */
    fun getPhysUnit(): String {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.PhysUnit)) {
            val res = this.optionalParameters[AxisDescrOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
            res.unit
        } else ""
    }

    /**
     * Checks and Returns the Step Size if set
     */
    fun getStepSize(): Float {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.StepSize)) {
            val res = this.optionalParameters[AxisDescrOpt.StepSize] as? StepSize ?: StepSize()
            res.size
        } else Float.NaN
    }

    /**
     * Checks and Returns a List of Annotations if set
     */
    fun getAnnotations(): List<Annotation> {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.Annotations)) {
            val result = this.optionalParameters[AxisDescrOpt.Annotations] as? List<*>
            result?.checkItemsAre<Annotation>() ?: listOf()
        } else listOf()
    }

    /**
     * Checks and Returns the Fix Axis Par if set
     */
    fun getFixAxisPar(): FixAxisPar {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.FixAxisPar)) {
            this.optionalParameters[AxisDescrOpt.FixAxisPar] as? FixAxisPar ?: FixAxisPar()
        } else FixAxisPar()
    }

    /**
     * Checks and Returns the Fix Axis Par Dist if set
     */
    fun getFixAxisParDist(): FixAxisParDist {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.FixAxisParDist)) {
            this.optionalParameters[AxisDescrOpt.FixAxisParDist] as? FixAxisParDist ?: FixAxisParDist()
        } else FixAxisParDist()
    }

    /**
     * Checks and Returns the Fix Axis Par List if set
     */
    fun getFixAxisParList(): List<String> {
        return if (this.optionalParameters.containsKey(AxisDescrOpt.FixAxisParList)) {
            val result = this.optionalParameters[AxisDescrOpt.FixAxisParList] as? FixAxisParList ?: FixAxisParList()
            result.list
        } else listOf()
    }
}