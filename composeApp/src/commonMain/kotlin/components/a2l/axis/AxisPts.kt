package components.a2l.axis

import components.a2l.ifdata.IfData
import components.a2l.memorysegment.MemorySegment
import components.a2l.recordlayout.RecordLayout
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.DisplayIdentifier
import components.a2l.classes.data.EcuAddressExtension
import components.a2l.classes.data.ExtendedLimits
import components.a2l.classes.data.Format
import components.a2l.classes.data.MemorySegmentReference
import components.a2l.classes.data.PhysUnit
import components.a2l.classes.data.StepSize
import components.a2l.classes.data.SymbolLink
import components.helpers.checkItemsAre
import components.a2l.classes.enums.ByteOrder
import components.a2l.classes.enums.CalibrationAccess
import components.a2l.classes.enums.ConversionType
import components.a2l.classes.enums.Deposit
import components.a2l.classes.enums.Monotony
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Contextual
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
) {
    fun isNotEmpty(): Boolean {
        return when {
            name.isBlank() -> false
            address == ULong.MIN_VALUE -> false
            else -> true
        }
    }

    /**
     * Checks and Returns a list of Annotations if set
     */
    fun getAnnotations(): List<Annotation> {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.Annotation)) {
            val result = this.optionalParameters[AxisPtsOpt.Annotation] as? List<*>
            result?.checkItemsAre<Annotation>() ?: listOf()
        } else listOf()
    }

    /**
     * Checks and Returns the ByteOrder if set
     */
    fun getByteOrder(): ByteOrder {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.ByteOrder)) {
            this.optionalParameters[AxisPtsOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
        } else ByteOrder.LITTLE_ENDIAN
    }

    // TODO: Check Conversion to see if its correct of if it should be a string

    /**
     * Checks and Returns the Deposit Structure if set
     */
    fun getDeposit(): Deposit {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.Deposit)) {
            this.optionalParameters[AxisPtsOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
        } else Deposit.NOT_SET
    }

    /**
     * Checks andReturns the Extended Limits if set
     */
    fun getExtendedLimits(): ExtendedLimits {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.ExtendedLimits)) {
            this.optionalParameters[AxisPtsOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
        } else ExtendedLimits()
    }

    /**
     * Checks and Returns Phys Unit if set
     */
    fun getPhysUnit(): String {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.PhysUnit)) {
            val res = this.optionalParameters[AxisPtsOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
            res.unit
        } else ""
    }

    /**
     * Checks and Returns True if Axis is Read Only
     */
    fun isReadOnly(): Boolean {
        return this.optionalParameters.containsKey(AxisPtsOpt.ReadOnly)
    }

    /**
     * Checks and returns the compu method for conversion
     */
    fun getRecordLayout(a2lData: A2lData): RecordLayout {
        return if (a2lData.recordLayouts.containsKey(this.recordLayout)) {
            a2lData.recordLayouts[this.recordLayout] ?: RecordLayout()
        } else RecordLayout()
    }

    /**
     * Checks and Returns the Monotony if set
     */
    fun getMonotony(): Monotony {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.Monotony)) {
            this.optionalParameters[AxisPtsOpt.Monotony] as? Monotony ?: Monotony.NOT_SET
        } else Monotony.NOT_SET
    }

    /**
     * Checks and Returns the Step Size if set
     */
    fun getStepSize(): Float {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.StepSize)) {
            val res = this.optionalParameters[AxisPtsOpt.StepSize] as? StepSize ?: StepSize()
            res.size
        } else Float.NaN
    }

    /**
     * Checks and Returns the Format if set
     */
    fun getFormat(): String {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.Format)) {
            val res = this.optionalParameters[AxisPtsOpt.Format] as? Format ?: Format()
            res.value
        } else ""
    }

    /**
     * Checks and Returns Calibration Access if set
     */
    fun getCalibrationAccess(): CalibrationAccess {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.CalibrationAccess)) {
            this.optionalParameters[AxisPtsOpt.CalibrationAccess] as? CalibrationAccess
                ?: CalibrationAccess.NOT_SET
        } else CalibrationAccess.NOT_SET
    }

    /**
     * Checks and Returns the Display Identifier if set
     */
    fun getDisplayIdentifier(): String {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.DisplayIdentifier)) {
            val res = this.optionalParameters[AxisPtsOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
            res.id
        } else ""
    }

    /**
     * Checks and Returns the Ecu Address Extension if set
     */
    fun getEcuAddressExtension(): String {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.EcuAddressExtension)) {
            val res = this.optionalParameters[AxisPtsOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
            res.address
        } else ""
    }

    /**
     * Checks and Returns a list of Functions if set
     */
    fun getFunctionList(): List<String> {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.FunctionList)) {
            val result = this.optionalParameters[AxisPtsOpt.FunctionList] as? List<*>
            // Check is items are correct type
            result?.checkItemsAre<String>() ?: listOf()
        } else listOf()
    }

    /**
     * Returns True if GuardRails is set
     */
    fun getGuardRails(): Boolean {
        return this.optionalParameters.containsKey(AxisPtsOpt.GuardRails)
    }

    /**
     * Checks and Returns any IfData if set
     */
    fun getIfData(): List<IfData> {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.IfData)) {
            val result = this.optionalParameters[AxisPtsOpt.IfData] as? List<*>
            // Check if items are correct type
            result?.checkItemsAre<IfData>() ?: listOf()
        } else listOf()
    }

    /**
     * Checks and Returns The Reference Memory Segment if set
     */
    fun getRefMemorySegment(a2lData: A2lData): MemorySegment {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.RefMemorySegment)) {
            val mRefMemorySegment = this.optionalParameters[AxisPtsOpt.RefMemorySegment] as? MemorySegmentReference ?: MemorySegmentReference()

            if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
                return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
            } else MemorySegment()
        } else MemorySegment()
    }

    /**
     * Checks and Returns the SymbolLink if set
     */
    fun getSymbolLink(): SymbolLink {
        return if (this.optionalParameters.containsKey(AxisPtsOpt.SymbolLink)) {
            this.optionalParameters[AxisPtsOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
        } else SymbolLink()
    }
}