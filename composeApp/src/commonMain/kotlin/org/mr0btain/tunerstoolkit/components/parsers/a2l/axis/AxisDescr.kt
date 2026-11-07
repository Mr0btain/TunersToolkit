@file:Suppress("ArrayInDataClass", "unused")

package org.mr0btain.tunerstoolkit.components.parsers.a2l.axis
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.CompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Annotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AxisPointReference
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.CurveReferenceAxis
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ExtendedLimits
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixAxisPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixAxisParDist
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixAxisParList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Format
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MaxGradient
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.PhysUnit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.StepSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.AxisType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Deposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Monotony
import org.mr0btain.tunerstoolkit.components.helpers.checkItemsAre
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
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
) : A2lOptional

fun AxisDescr.validate(): Boolean {
    return when {
        type == AxisType.NOT_SET -> false
        else -> true
    }
}

/**
 * Checks and Returns the Format if set
 */
fun AxisDescr.getFormat(): String {
    return if (optionalParameters.containsKey(AxisDescrOpt.Format)) {
        val res = optionalParameters[AxisDescrOpt.Format] as? Format ?: Format()
        res.value
    } else ""
}

/**
 * Checks and Returns the Reference Axis Point if set
 */
fun AxisDescr.getRefAxisPoint(a2lData: A2lData): AxisPts {
    return if (optionalParameters.containsKey(AxisDescrOpt.ReferenceAxisPoint)) {
        val result = optionalParameters[AxisDescrOpt.ReferenceAxisPoint] as? AxisPointReference ?: AxisPointReference()

        if (result != AxisPointReference() && a2lData.axisPoints.containsKey(result.name)) {
            return a2lData.axisPoints[result.name] ?: AxisPts()
        } else AxisPts()
    } else AxisPts()
}

/**
 * Returns True if Read Only is set
 */
fun AxisDescr.isReadOnly(): Boolean {
    return optionalParameters.containsKey(AxisDescrOpt.ReadOnly)
}

/**
 * Checks and Returns the Maximum Gradient Allowed if set
 */
fun AxisDescr.getMaxGradient(): Double {
    return if (optionalParameters.containsKey(AxisDescrOpt.MaxGradient)) {
        val res = optionalParameters[AxisDescrOpt.MaxGradient] as? MaxGradient ?: MaxGradient()
        res.value
    } else Double.NaN
}

/**
 * Checks and Returns the Monotony if set
 */
fun AxisDescr.getMonotony(): Monotony {
    return if (optionalParameters.containsKey(AxisDescrOpt.Monotony)) {
        optionalParameters[AxisDescrOpt.Monotony] as? Monotony ?: Monotony.NOT_SET
    } else Monotony.NOT_SET
}

/**
 * Checks and Returns the Byte order if set
 */
fun AxisDescr.getByteOrder(): ByteOrder {
    return if (optionalParameters.containsKey(AxisDescrOpt.ByteOrder)) {
        optionalParameters[AxisDescrOpt.Monotony] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
    } else ByteOrder.LITTLE_ENDIAN
}

/**
 * Checks and returns the compu method for conversion
 */
fun AxisDescr.getCompuMethod(a2lData: A2lData): CompuMethod {
    return if (a2lData.compuMethods.containsKey(conversion)) {
        a2lData.compuMethods[conversion] ?: CompuMethod()
    } else CompuMethod()
}

/**
 * Checks and Returns the Extended Limits if set
 */
fun AxisDescr.getExtendedLimits(): ExtendedLimits {
    return if (optionalParameters.containsKey(AxisDescrOpt.ExtendedLimits)) {
        optionalParameters[AxisDescrOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
    } else ExtendedLimits()
}

/**
 * Checks and Returns the Reference Curve Axis if set
 */
fun AxisDescr.getReferenceCurveAxis(a2lData: A2lData): Characteristic {
    return if (optionalParameters.containsKey(AxisDescrOpt.ReferenceCurveAxis)) {
        val mRefCurve = optionalParameters[AxisDescrOpt.ReferenceCurveAxis] as? CurveReferenceAxis
            ?: CurveReferenceAxis()

        if (mRefCurve.reference.isNotBlank() && a2lData.characteristics.containsKey(mRefCurve.reference)) {
            return a2lData.characteristics[mRefCurve.reference] ?: Characteristic()
        } else Characteristic()
    } else Characteristic()
}

/**
 * Checks and Returns the Deposit of the values in Memory if set
 */
fun AxisDescr.getDeposit(): Deposit {
    return if (optionalParameters.containsKey(AxisDescrOpt.Deposit)) {
        optionalParameters[AxisDescrOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
    } else Deposit.NOT_SET
}

/**
 * Checks and Returns the Phys Unit if set
 */
fun AxisDescr.getPhysUnit(): String {
    return if (optionalParameters.containsKey(AxisDescrOpt.PhysUnit)) {
        val res = optionalParameters[AxisDescrOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
        res.unit
    } else ""
}

/**
 * Checks and Returns the Step Size if set
 */
fun AxisDescr.getStepSize(): Float {
    return if (optionalParameters.containsKey(AxisDescrOpt.StepSize)) {
        val res = optionalParameters[AxisDescrOpt.StepSize] as? StepSize ?: StepSize()
        res.size
    } else Float.NaN
}

/**
 * Checks and Returns a List of Annotations if set
 */
fun AxisDescr.getAnnotations(): List<Annotation> {
    return if (optionalParameters.containsKey(AxisDescrOpt.Annotations)) {
        val result = optionalParameters[AxisDescrOpt.Annotations] as? List<*>
        result?.checkItemsAre<Annotation>() ?: listOf()
    } else listOf()
}

/**
 * Checks and Returns the Fix Axis Par if set
 */
fun AxisDescr.getFixAxisPar(): FixAxisPar {
    return if (optionalParameters.containsKey(AxisDescrOpt.FixAxisPar)) {
        optionalParameters[AxisDescrOpt.FixAxisPar] as? FixAxisPar ?: FixAxisPar()
    } else FixAxisPar()
}

/**
 * Checks and Returns the Fix Axis Par Dist if set
 */
fun AxisDescr.getFixAxisParDist(): FixAxisParDist {
    return if (optionalParameters.containsKey(AxisDescrOpt.FixAxisParDist)) {
        optionalParameters[AxisDescrOpt.FixAxisParDist] as? FixAxisParDist ?: FixAxisParDist()
    } else FixAxisParDist()
}

/**
 * Checks and Returns the Fix Axis Par List if set
 */
fun AxisDescr.getFixAxisParList(): List<String> {
    return if (optionalParameters.containsKey(AxisDescrOpt.FixAxisParList)) {
        val result = optionalParameters[AxisDescrOpt.FixAxisParList] as? FixAxisParList ?: FixAxisParList()
        result.list
    } else listOf()
}