package org.mr0btain.tunerstoolkit.components.parsers.a2l.axis
import org.mr0btain.tunerstoolkit.components.parsers.a2l.memorysegment.MemorySegment
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.RecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Annotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DisplayIdentifier
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.EcuAddressExtension
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ExtendedLimits
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Format
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MemorySegmentReference
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.PhysUnit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.StepSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SymbolLink
import org.mr0btain.tunerstoolkit.components.helpers.checkItemsAre
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CalibrationAccess
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ConversionType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Deposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Monotony
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * Specification of parameters for the handling of an axis points distribution
 * @param name unique identifier in the ECU program
 * @param longIdentifier Description of the item
 * @param address address of the adjustable object in the emulation memory
 * @param inputQuantity reference to the data record for description of the input
 * quantity (see MEASUREMENT). If there is no input quantity assigned, parameter 'InputQuantity'
 * should be set to "NO_INPUT_QUANTITY" (measurement and calibration systems must be capable to
 * treat this case).
 * @param recordLayout reference to the relevant data record for description of
 * the record layout (see RECORD_LAYOUT)
 * @param maxDiff maximum float with respect to the adjustment of a table value
 * @param conversion Reference to the relevant record of the description of
 * the conversion method (see COMPU_METHOD). If there is no conversion method, as in the case
 * of CURVE_AXIS, the parameter Conversion should be set to ―NO_COMPU_METHOD (measurement and
 * calibration systems must be able to handle this case).
 * @param maxAxisPoints maximum number of axis points
 * @param lowerLimit plausible range of axis point values, lower limit
 * @param upperLimit plausible range of axis point values, upper limit
 * Note: Depending on the type of conversion, the limit
 * values are interpreted as physical or internal values.
 * For conversions of type COMPU_VTAB and COMPU_VTAB_RANGE, the limit values are
 * interpreted as internal values. For all other conversion types, the limit values are
 * interpreted as physical values. Note: AXIS_PTS and AXIS_DESCR define the
 * same parameters. Which parameters are dominate is described at AXIS_DESCR
 */

@Serializable
data class AxisPts(
    val name: String = "",
    val longIdentifier: String = "",
    val address: ULong = ULong.MIN_VALUE,
    val inputQuantity: String = "",
    val recordLayout: String = "",
    val maxDiff: Double = Double.NaN,
    val conversion: ConversionType = ConversionType.NOT_SET,
    val maxAxisPoints: UInt = UInt.MIN_VALUE,
    val lowerLimit: Double = Double.NaN,
    val upperLimit: Double = Double.NaN,
    val optionalParameters: Map<AxisPtsOpt, A2lOptional> = mapOf()
)

fun AxisPts.validate(): Boolean {
    return when {
        name.isBlank() -> false
        address == ULong.MIN_VALUE -> false
        else -> true
    }
}

/**
 * Checks and Returns a list of Annotations if set
 */
fun AxisPts.getAnnotations(): List<Annotation> {
    return if (optionalParameters.containsKey(AxisPtsOpt.Annotation)) {
        val result = optionalParameters[AxisPtsOpt.Annotation] as? List<*>
        result?.checkItemsAre<Annotation>() ?: listOf()
    } else listOf()
}

/**
 * Checks and Returns the ByteOrder if set
 */
fun AxisPts.getByteOrder(): ByteOrder {
    return if (optionalParameters.containsKey(AxisPtsOpt.ByteOrder)) {
        optionalParameters[AxisPtsOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
    } else ByteOrder.LITTLE_ENDIAN
}

// TODO: Check Conversion to see if its correct of if it should be a string

/**
 * Checks and Returns the Deposit Structure if set
 */
fun AxisPts.getDeposit(): Deposit {
    return if (optionalParameters.containsKey(AxisPtsOpt.Deposit)) {
        optionalParameters[AxisPtsOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
    } else Deposit.NOT_SET
}

/**
 * Checks andReturns the Extended Limits if set
 */
fun AxisPts.getExtendedLimits(): ExtendedLimits {
    return if (optionalParameters.containsKey(AxisPtsOpt.ExtendedLimits)) {
        optionalParameters[AxisPtsOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
    } else ExtendedLimits()
}

/**
 * Checks and Returns Phys Unit if set
 */
fun AxisPts.getPhysUnit(): String {
    return if (optionalParameters.containsKey(AxisPtsOpt.PhysUnit)) {
        val res = optionalParameters[AxisPtsOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
        res.unit
    } else ""
}

/**
 * Checks and Returns True if Axis is Read Only
 */
fun AxisPts.isReadOnly(): Boolean {
    return optionalParameters.containsKey(AxisPtsOpt.ReadOnly)
}

/**
 * Checks and returns the compu method for conversion
 */
fun AxisPts.getRecordLayout(a2lData: A2lData): RecordLayout {
    return if (a2lData.recordLayouts.containsKey(recordLayout)) {
        a2lData.recordLayouts[recordLayout] ?: RecordLayout()
    } else RecordLayout()
}

/**
 * Checks and Returns the Monotony if set
 */
fun AxisPts.getMonotony(): Monotony {
    return if (optionalParameters.containsKey(AxisPtsOpt.Monotony)) {
        optionalParameters[AxisPtsOpt.Monotony] as? Monotony ?: Monotony.NOT_SET
    } else Monotony.NOT_SET
}

/**
 * Checks and Returns the Step Size if set
 */
fun AxisPts.getStepSize(): Float {
    return if (optionalParameters.containsKey(AxisPtsOpt.StepSize)) {
        val res = optionalParameters[AxisPtsOpt.StepSize] as? StepSize ?: StepSize()
        res.size
    } else Float.NaN
}

/**
 * Checks and Returns the Format if set
 */
fun AxisPts.getFormat(): String {
    return if (optionalParameters.containsKey(AxisPtsOpt.Format)) {
        val res = optionalParameters[AxisPtsOpt.Format] as? Format ?: Format()
        res.value
    } else ""
}

/**
 * Checks and Returns Calibration Access if set
 */
fun AxisPts.getCalibrationAccess(): CalibrationAccess {
    return if (optionalParameters.containsKey(AxisPtsOpt.CalibrationAccess)) {
        optionalParameters[AxisPtsOpt.CalibrationAccess] as? CalibrationAccess
            ?: CalibrationAccess.NOT_SET
    } else CalibrationAccess.NOT_SET
}

/**
 * Checks and Returns the Display Identifier if set
 */
fun AxisPts.getDisplayIdentifier(): String {
    return if (optionalParameters.containsKey(AxisPtsOpt.DisplayIdentifier)) {
        val res = optionalParameters[AxisPtsOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
        res.id
    } else ""
}

/**
 * Checks and Returns the Ecu Address Extension if set
 */
fun AxisPts.getEcuAddressExtension(): String {
    return if (optionalParameters.containsKey(AxisPtsOpt.EcuAddressExtension)) {
        val res = optionalParameters[AxisPtsOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
        res.address
    } else ""
}

/**
 * Checks and Returns a list of Functions if set
 */
fun AxisPts.getFunctionList(): List<String> {
    return if (optionalParameters.containsKey(AxisPtsOpt.FunctionList)) {
        val result = optionalParameters[AxisPtsOpt.FunctionList] as? List<*>
        // Check is items are correct type
        result?.checkItemsAre<String>() ?: listOf()
    } else listOf()
}

/**
 * Returns True if GuardRails is set
 */
fun AxisPts.getGuardRails(): Boolean {
    return optionalParameters.containsKey(AxisPtsOpt.GuardRails)
}

/**
 * Checks and Returns any IfData if set
 */
fun AxisPts.getIfData(): List<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData> {
    return if (optionalParameters.containsKey(AxisPtsOpt.IfData)) {
        val result = optionalParameters[AxisPtsOpt.IfData] as? List<*>
        // Check if items are correct type
        result?.checkItemsAre<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData>() ?: listOf()
    } else listOf()
}

/**
 * Checks and Returns The Reference Memory Segment if set
 */
fun AxisPts.getRefMemorySegment(a2lData: A2lData): MemorySegment {
    return if (optionalParameters.containsKey(AxisPtsOpt.RefMemorySegment)) {
        val mRefMemorySegment = optionalParameters[AxisPtsOpt.RefMemorySegment] as? MemorySegmentReference
            ?: MemorySegmentReference()

        if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
            return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
        } else MemorySegment()
    } else MemorySegment()
}

/**
 * Checks and Returns the SymbolLink if set
 */
fun AxisPts.getSymbolLink(): SymbolLink {
    return if (optionalParameters.containsKey(AxisPtsOpt.SymbolLink)) {
        optionalParameters[AxisPtsOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
    } else SymbolLink()
}