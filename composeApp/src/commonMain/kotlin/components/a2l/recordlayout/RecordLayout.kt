package components.a2l.recordlayout

import components.a2l.classes.RecordLayoutAxisPoint
import components.a2l.classes.RecordLayoutAxisRescale
import components.a2l.classes.RecordLayoutIdentification
import components.a2l.classes.RecordLayoutOffset
import components.a2l.classes.RecordLayoutReserved
import components.a2l.classes.data.AlignmentByte
import components.a2l.classes.data.AlignmentFloat16
import components.a2l.classes.data.AlignmentFloat32
import components.a2l.classes.data.AlignmentFloat64
import components.a2l.classes.data.AlignmentInt
import components.a2l.classes.data.AlignmentLong
import components.a2l.classes.data.AlignmentWord
import components.a2l.classes.data.DistOp
import components.a2l.classes.data.ExtendedLimits
import components.a2l.classes.data.FixNoAxisPts
import components.a2l.classes.data.FncValues
import components.a2l.classes.data.NoAxisPts
import components.a2l.classes.data.NoRescale
import components.a2l.classes.data.RipAddress
import components.a2l.classes.data.ShiftOp
import components.a2l.classes.data.SrcAddress
import components.a2l.objects.A2lOptional
import components.calibrationdata.classes.AxisDataType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * RecordLayout data type.
 *
 * This is used to describe the layout in memory of the item that refers to it.
 */

@Serializable
data class RecordLayout(
    val name: String = "",
    val nameSet: Boolean = false,
    val optionalParameters: Map<RecordLOpt, A2lOptional> = mapOf()
) {
    fun isNotEmpty(): Boolean {
        return when {
            name.isBlank() -> false
            optionalParameters.isEmpty() -> false
            else -> true
        }
    }

    /**
     * Checks and Returns the Alignment Byte if set
     */
    fun getAlignmentByte(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentByte)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentByte] as? AlignmentByte ?: AlignmentByte()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Float 16 if set
     */
    fun getAlignmentFloat16Ieee(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentFloat16Ieee)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentFloat16Ieee] as? AlignmentFloat16 ?: AlignmentFloat16()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Float 32 if set
     */
    fun getAlignmentFloat32Ieee(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentFloat32Ieee)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentFloat32Ieee] as? AlignmentFloat32 ?: AlignmentFloat32()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Float 64 if set
     */
    fun getAlignmentFloat64Ieee(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentFloat64Ieee)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentFloat64Ieee] as? AlignmentFloat64 ?: AlignmentFloat64()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Int 64 if set
     */
    fun getAlignmentInt64(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentInt64)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentInt64] as? AlignmentInt ?: AlignmentInt()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Long if set
     */
    fun getAlignmentLong(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentLong)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentLong] as? AlignmentLong ?: AlignmentLong()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns the Alignment Word if set
     */
    fun getAlignmentWord(): Int {
        return if (this.optionalParameters.containsKey(RecordLOpt.AlignmentWord)) {
            val res = this.optionalParameters[RecordLOpt.AlignmentWord] as? AlignmentWord ?: AlignmentWord()
            res.num
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns AXIS_PTS_X if set
     */
    fun getAxisPtsX(): RecordLayoutAxisPoint {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_X)) {
            this.optionalParameters[RecordLOpt.AXIS_PTS_X] as? RecordLayoutAxisPoint
                ?: RecordLayoutAxisPoint()
        } else RecordLayoutAxisPoint()
    }

    /**
     * Checks and Returns AXIS_PTS_Y if set
     */
    private fun getAxisPtsY(): RecordLayoutAxisPoint {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_Y)) {
            this.optionalParameters[RecordLOpt.AXIS_PTS_Y] as? RecordLayoutAxisPoint
                ?: RecordLayoutAxisPoint()
        } else RecordLayoutAxisPoint()
    }

    /**
     * Checks and Returns AXIS_PTS_Z if set
     */
    private fun getAxisPtsZ(): RecordLayoutAxisPoint {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_Z)) {
            this.optionalParameters[RecordLOpt.AXIS_PTS_Z] as? RecordLayoutAxisPoint
                ?: RecordLayoutAxisPoint()
        } else RecordLayoutAxisPoint()
    }

    /**
     * Checks and Returns AXIS_PTS_4 if set
     */
    private fun getAxisPts4(): RecordLayoutAxisPoint {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_4)) {
            this.optionalParameters[RecordLOpt.AXIS_PTS_4] as? RecordLayoutAxisPoint
                ?: RecordLayoutAxisPoint()
        } else RecordLayoutAxisPoint()
    }

    /**
     * Checks and Returns AXIS_PTS_5 if set
     */
    private fun getAxisPts5(): RecordLayoutAxisPoint {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_5)) {
            this.optionalParameters[RecordLOpt.AXIS_PTS_5] as? RecordLayoutAxisPoint
                ?: RecordLayoutAxisPoint()
        } else RecordLayoutAxisPoint()
    }

    /**
     * Checks and Returns AXIS_RESCALE_X if set
     */
    fun getAxisRescaleX(): RecordLayoutAxisRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_X)) {
            this.optionalParameters[RecordLOpt.AXIS_RESCALE_X] as? RecordLayoutAxisRescale
                ?: RecordLayoutAxisRescale()
        } else RecordLayoutAxisRescale()
    }

    /**
     * Checks and Returns AXIS_RESCALE_Y if set
     */
    private fun getAxisRescaleY(): RecordLayoutAxisRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_Y)) {
            this.optionalParameters[RecordLOpt.AXIS_RESCALE_Y] as? RecordLayoutAxisRescale
                ?: RecordLayoutAxisRescale()
        } else RecordLayoutAxisRescale()
    }

    /**
     * Checks and Returns AXIS_RESCALE_Z if set
     */
    private fun getAxisRescaleZ(): RecordLayoutAxisRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_Z)) {
            this.optionalParameters[RecordLOpt.AXIS_RESCALE_Z] as? RecordLayoutAxisRescale
                ?: RecordLayoutAxisRescale()
        } else RecordLayoutAxisRescale()
    }

    /**
     * Checks and Returns AXIS_RESCALE_4 if set
     */
    private fun getAxisRescale4(): RecordLayoutAxisRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_4)) {
            this.optionalParameters[RecordLOpt.AXIS_RESCALE_4] as? RecordLayoutAxisRescale
                ?: RecordLayoutAxisRescale()
        } else RecordLayoutAxisRescale()
    }

    /**
     * Checks and Returns AXIS_RESCALE_5 if set
     */
    private fun getAxisRescale5(): RecordLayoutAxisRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_5)) {
            this.optionalParameters[RecordLOpt.AXIS_RESCALE_5] as? RecordLayoutAxisRescale
                ?: RecordLayoutAxisRescale()
        } else RecordLayoutAxisRescale()
    }

    /**
     * Checks and Returns true if Calibration Access is set
     */
    fun getCalibrationAccess(): Boolean {
        return this.optionalParameters.containsKey(RecordLOpt.CalibrationAccess)
    }

    /**
     * Checks and Returns DIST_OP_X if set
     */
    private fun getDistOpX(): DistOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.DIST_OP_X)) {
            this.optionalParameters[RecordLOpt.DIST_OP_X] as? DistOp ?: DistOp()
        } else DistOp()
    }

    /**
     * Checks and Returns DIST_OP_Y if set
     */
    private fun getDistOpY(): DistOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.DIST_OP_Y)) {
            this.optionalParameters[RecordLOpt.DIST_OP_Y] as? DistOp ?: DistOp()
        } else DistOp()
    }

    /**
     * Checks and Returns DIST_OP_Z if set
     */
    private fun getDistOpZ(): DistOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.DIST_OP_Z)) {
            this.optionalParameters[RecordLOpt.DIST_OP_Z] as? DistOp ?: DistOp()
        } else DistOp()
    }

    /**
     * Checks and Returns DIST_OP_4 if set
     */
    private fun getDistOp4(): DistOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.DIST_OP_4)) {
            this.optionalParameters[RecordLOpt.DIST_OP_4] as? DistOp ?: DistOp()
        } else DistOp()
    }

    /**
     * Checks and Returns DIST_OP_5 if set
     */
    private fun getDistOp5(): DistOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.DIST_OP_5)) {
            this.optionalParameters[RecordLOpt.DIST_OP_5] as? DistOp ?: DistOp()
        } else DistOp()
    }

    /**
     * Checks andReturns the Extended Limits if set
     */
    fun getExtendedLimits(): ExtendedLimits {
        return if (this.optionalParameters.containsKey(RecordLOpt.ExtendedLimits)) {
            this.optionalParameters[RecordLOpt.ExtendedLimits] as? ExtendedLimits
                ?: ExtendedLimits()
        } else ExtendedLimits()
    }

    /**
     * Checks and Returns FIX_NO_AXIS_X if set
     */
    fun getFixNoAxisX(): FixNoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_X)) {
            this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_X] as? FixNoAxisPts
                ?: FixNoAxisPts()
        } else FixNoAxisPts()
    }

    /**
     * Checks and Returns FIX_NO_AXIS_Y if set
     */
    fun getFixNoAxisY(): FixNoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_Y)) {
            this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_Y] as? FixNoAxisPts
                ?: FixNoAxisPts()
        } else FixNoAxisPts()
    }

    /**
     * Checks and Returns FIX_NO_AXIS_Z if set
     */
    fun getFixNoAxisZ(): FixNoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_Z)) {
            this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_Z] as? FixNoAxisPts
                ?: FixNoAxisPts()
        } else FixNoAxisPts()
    }

    /**
     * Checks and Returns FIX_NO_AXIS_4 if set
     */
    fun getFixNoAxis4(): FixNoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_4)) {
            this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_4] as? FixNoAxisPts
                ?: FixNoAxisPts()
        } else FixNoAxisPts()
    }

    /**
     * Checks and Returns FIX_NO_AXIS_5 if set
     */
    fun getFixNoAxis5(): FixNoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_5)) {
            this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_5] as? FixNoAxisPts
                ?: FixNoAxisPts()
        } else FixNoAxisPts()
    }

    /**
     * Checks and Returns the FNC Values if set
     */
    fun getFncValues(): FncValues {
        return if (this.optionalParameters.containsKey(RecordLOpt.FncValues)) {
            this.optionalParameters[RecordLOpt.FncValues] as? FncValues ?: FncValues()
        } else FncValues()
    }

    /**
     * Returns True if GuardRails is set
     */
    fun getGuardRails(): Boolean {
        return this.optionalParameters.containsKey(RecordLOpt.GuardRails)
    }

    /**
     * Checks and Returns Identification if set
     */
    fun getIdentification(): RecordLayoutIdentification {
        return if (this.optionalParameters.containsKey(RecordLOpt.Identification)) {
            this.optionalParameters[RecordLOpt.Identification] as? RecordLayoutIdentification
                ?: RecordLayoutIdentification()
        } else RecordLayoutIdentification()
    }

    /**
     * Checks and Returns NO_AXIS_PTS_X if set
     */
    fun getNoAxisPtsX(): NoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_X)) {
            this.optionalParameters[RecordLOpt.NO_AXIS_PTS_X] as? NoAxisPts ?: NoAxisPts()
        } else NoAxisPts()
    }

    /**
     * Checks and Returns NO_AXIS_PTS_Y if set
     */
    fun getNoAxisPtsY(): NoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_Y)) {
            this.optionalParameters[RecordLOpt.NO_AXIS_PTS_Y] as? NoAxisPts ?: NoAxisPts()
        } else NoAxisPts()
    }

    /**
     * Checks and Returns NO_AXIS_PTS_Z if set
     */
    fun getNoAxisPtsZ(): NoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_Z)) {
            this.optionalParameters[RecordLOpt.NO_AXIS_PTS_Z] as? NoAxisPts ?: NoAxisPts()
        } else NoAxisPts()
    }

    /**
     * Checks and Returns NO_AXIS_PTS_4 if set
     */
    fun getNoAxisPts4(): NoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_4)) {
            this.optionalParameters[RecordLOpt.NO_AXIS_PTS_4] as? NoAxisPts ?: NoAxisPts()
        } else NoAxisPts()
    }

    /**
     * Checks and Returns NO_AXIS_PTS_5 if set
     */
    fun getNoAxisPts5(): NoAxisPts {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_5)) {
            this.optionalParameters[RecordLOpt.NO_AXIS_PTS_5] as? NoAxisPts ?: NoAxisPts()
        } else NoAxisPts()
    }

    /**
     * Checks and Returns NO_RESCALE_X if set
     */
    private fun getNoRescaleX(): NoRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_X)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_X] as? NoRescale ?: NoRescale()
        } else NoRescale()
    }

    /**
     * Checks and Returns NO_RESCALE_Y if set
     */
    private fun getNoRescaleY(): NoRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Y)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_Y] as? NoRescale ?: NoRescale()
        } else NoRescale()
    }

    /**
     * Checks and Returns NO_RESCALE_Z if set
     */
    private fun getNoRescaleZ(): NoRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Z)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_Z] as? NoRescale ?: NoRescale()
        } else NoRescale()
    }

    /**
     * Checks and Returns NO_RESCALE_4 if set
     */
    private fun getNoRescale4(): NoRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_4)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_4] as? NoRescale ?: NoRescale()
        } else NoRescale()
    }

    /**
     * Checks and Returns NO_RESCALE_5 if set
     */
    private fun getNoRescale5(): NoRescale {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_5)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_5] as? NoRescale ?: NoRescale()
        } else NoRescale()
    }

    /**
     * Checks and Returns OFFSET_X if set
     */
    private fun getOffsetX(): RecordLayoutOffset {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_X)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_X] as? RecordLayoutOffset
                ?: RecordLayoutOffset()
        } else RecordLayoutOffset()
    }

    /**
     * Checks and Returns OFFSET_Y if set
     */
    private fun getOffsetY(): RecordLayoutOffset {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Y)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_Y] as? RecordLayoutOffset
                ?: RecordLayoutOffset()
        } else RecordLayoutOffset()
    }

    /**
     * Checks and Returns OFFSET_Z if set
     */
    private fun getOffsetZ(): RecordLayoutOffset {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Z)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_Z] as? RecordLayoutOffset
                ?: RecordLayoutOffset()
        } else RecordLayoutOffset()
    }

    /**
     * Checks and Returns OFFSET_4 if set
     */
    private fun getOffset4(): RecordLayoutOffset {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_4)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_4] as? RecordLayoutOffset
                ?: RecordLayoutOffset()
        } else RecordLayoutOffset()
    }

    /**
     * Checks and Returns OFFSET_5 if set
     */
    private fun getOffset5(): RecordLayoutOffset {
        return if (this.optionalParameters.containsKey(RecordLOpt.NO_RESCALE_5)) {
            this.optionalParameters[RecordLOpt.NO_RESCALE_5] as? RecordLayoutOffset
                ?: RecordLayoutOffset()
        } else RecordLayoutOffset()
    }

    /**
     * Checks and Returns the Reserved if set
     */
    fun getReserved(): RecordLayoutReserved {
        return if (this.optionalParameters.containsKey(RecordLOpt.Reserved)) {
            this.optionalParameters[RecordLOpt.Reserved] as? RecordLayoutReserved
                ?: RecordLayoutReserved()
        } else RecordLayoutReserved()
    }

    /**
     * Checks and Returns RIP_ADDR_W if set
     */
    private fun getRipAddrW(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_W)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_W] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and Returns RIP_ADDR_X if set
     */
    private fun getRipAddrX(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_X)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_X] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and Returns RIP_ADDR_Y if set
     */
    private fun getRipAddrY(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_Y)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_Y] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and Returns RIP_ADDR_Z if set
     */
    private fun getRipAddrZ(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_Z)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_Z] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and Returns RIP_ADDR_4 if set
     */
    private fun getRipAddr4(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_4)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_4] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and Returns RIP_ADDR_5 if set
     */
    private fun getRipAddr5(): RipAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.RIP_ADDR_5)) {
            this.optionalParameters[RecordLOpt.RIP_ADDR_5] as? RipAddress ?: RipAddress()
        } else RipAddress()
    }

    /**
     * Checks and returns an AxisPts if set
     */
    fun getAxisPts(type: AxisDataType): RecordLayoutAxisPoint {
        return when (type) {
            AxisDataType.X -> this.getAxisPtsX()
            AxisDataType.Y -> this.getAxisPtsY()
            AxisDataType.Z -> this.getAxisPtsZ()
            AxisDataType.Axis4 -> this.getAxisPts4()
            AxisDataType.Axis5 -> this.getAxisPts5()
            else -> RecordLayoutAxisPoint()
        }
    }

    /**
     * Checks and returns an AxisRescale if set
     */
    fun getAxisRescale(type: AxisDataType): RecordLayoutAxisRescale {
        return when (type) {
            AxisDataType.X -> this.getAxisRescaleX()
            AxisDataType.Y -> this.getAxisRescaleY()
            AxisDataType.Z -> this.getAxisRescaleZ()
            AxisDataType.Axis4 -> this.getAxisRescale4()
            AxisDataType.Axis5 -> this.getAxisRescale5()
            else -> RecordLayoutAxisRescale()
        }
    }

    /**
     * Checks and returns NoRescale if set
     */
    fun getNoRescale(type: AxisDataType): NoRescale {
        return when (type) {
            AxisDataType.X -> this.getNoRescaleX()
            AxisDataType.Y -> this.getNoRescaleY()
            AxisDataType.Z -> this.getNoRescaleZ()
            AxisDataType.Axis4 -> this.getNoRescale4()
            AxisDataType.Axis5 -> this.getNoRescale5()
            else -> NoRescale()
        }
    }

    /**
     * Checks and returns DistOp if set
     */
    fun getDistOp(type: AxisDataType): DistOp {
        return when (type) {
            AxisDataType.X -> this.getDistOpX()
            AxisDataType.Y -> this.getDistOpY()
            AxisDataType.Z -> this.getDistOpZ()
            AxisDataType.Axis4 -> this.getDistOp4()
            AxisDataType.Axis5 -> this.getDistOp5()
            else -> DistOp()
        }
    }

    /**
     * Checks and returns FixNoAxisPts if set
     */
    fun getFixNoAxisPts(type: AxisDataType): FixNoAxisPts {
        return when (type) {
            AxisDataType.X -> this.getFixNoAxisX()
            AxisDataType.Y -> this.getFixNoAxisY()
            AxisDataType.Z -> this.getFixNoAxisZ()
            AxisDataType.Axis4 -> this.getFixNoAxis4()
            AxisDataType.Axis5 -> this.getFixNoAxis5()
            else -> FixNoAxisPts()
        }
    }

    /**
     * Checks and returns NoAxisPts if set
     */
    fun getNoAxisPts(type: AxisDataType): NoAxisPts {
        return when (type) {
            AxisDataType.X -> this.getNoAxisPtsX()
            AxisDataType.Y -> this.getNoAxisPtsY()
            AxisDataType.Z -> this.getNoAxisPtsZ()
            AxisDataType.Axis4 -> this.getNoAxisPts4()
            AxisDataType.Axis5 -> this.getNoAxisPts5()
            else -> NoAxisPts()
        }
    }

    /**
     * Checks and returns Offset if set
     */
    fun getOffset(type: AxisDataType): RecordLayoutOffset {
        return when (type) {
            AxisDataType.X -> this.getOffsetX()
            AxisDataType.Y -> this.getOffsetY()
            AxisDataType.Z -> this.getOffsetZ()
            AxisDataType.Axis4 -> this.getOffset4()
            AxisDataType.Axis5 -> this.getOffset5()
            else -> RecordLayoutOffset()
        }
    }

    /**
     * Checks and returns RipAddr if set
     */
    fun getRipAddr(type: AxisDataType): RipAddress {
        return when (type) {
            AxisDataType.W -> this.getRipAddrW()
            AxisDataType.X -> this.getRipAddrX()
            AxisDataType.Y -> this.getRipAddrY()
            AxisDataType.Z -> this.getRipAddrZ()
            AxisDataType.Axis4 -> this.getRipAddr4()
            AxisDataType.Axis5 -> this.getRipAddr5()
            else -> RipAddress()
        }
    }

    /**
     * Checks and returns ShiftOp if set
     */
    fun getShiftOp(type: AxisDataType): ShiftOp {
        return when (type) {
            AxisDataType.X -> this.getShiftOpX()
            AxisDataType.Y -> this.getShiftOpY()
            AxisDataType.Z -> this.getShiftOpZ()
            AxisDataType.Axis4 -> this.getShiftOpX4()
            AxisDataType.Axis5 -> this.getShiftOp5()
            else -> ShiftOp()
        }
    }

    /**
     * Checks and returns SrcAddr if set
     */
    fun getSrcAddr(type: AxisDataType): SrcAddress {
        return when (type) {
            AxisDataType.X -> this.getSrcAddrX()
            AxisDataType.Y -> this.getSrcAddrY()
            AxisDataType.Z -> this.getSrcAddrZ()
            AxisDataType.Axis4 -> this.getSrcAddr4()
            AxisDataType.Axis5 -> this.getSrcAddr5()
            else -> SrcAddress()
        }
    }

    /**
     * Checks and Returns SRC_ADDR_X if set
     */
    fun getSrcAddrX(): SrcAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_X)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_X] as? SrcAddress ?: SrcAddress()
        } else SrcAddress()
    }

    /**
     * Checks and Returns SRC_ADDR_Y if set
     */
    fun getSrcAddrY(): SrcAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Y)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_Y] as? SrcAddress ?: SrcAddress()
        } else SrcAddress()
    }

    /**
     * Checks and Returns SRC_ADDR_Z if set
     */
    fun getSrcAddrZ(): SrcAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Z)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_Z] as? SrcAddress ?: SrcAddress()
        } else SrcAddress()
    }

    /**
     * Checks and Returns SRC_ADDR_4 if set
     */
    fun getSrcAddr4(): SrcAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_4)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_4] as? SrcAddress ?: SrcAddress()
        } else SrcAddress()
    }

    /**
     * Checks and Returns SRC_ADDR_5 if set
     */
    fun getSrcAddr5(): SrcAddress {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_5)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_5] as? SrcAddress ?: SrcAddress()
        } else SrcAddress()
    }

    /**
     * Checks and Returns SHIFT_OP_X if set
     */
    fun getShiftOpX(): ShiftOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_X)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_X] as? ShiftOp ?: ShiftOp()
        } else ShiftOp()
    }

    /**
     * Checks and Returns SHIFT_OP_Y if set
     */
    private fun getShiftOpY(): ShiftOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Y)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_Y] as? ShiftOp ?: ShiftOp()
        } else ShiftOp()
    }

    /**
     * Checks and Returns SHIFT_OP_Z if set
     */
    private fun getShiftOpZ(): ShiftOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Z)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_Z] as? ShiftOp ?: ShiftOp()
        } else ShiftOp()
    }

    /**
     * Checks and Returns SHIFT_OP_4 if set
     */
    private fun getShiftOpX4(): ShiftOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_4)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_4] as? ShiftOp ?: ShiftOp()
        } else ShiftOp()
    }

    /**
     * Checks and Returns SHIFT_OP_5 if set
     */
    private fun getShiftOp5(): ShiftOp {
        return if (this.optionalParameters.containsKey(RecordLOpt.SRC_ADDR_5)) {
            this.optionalParameters[RecordLOpt.SRC_ADDR_5] as? ShiftOp ?: ShiftOp()
        } else ShiftOp()
    }

    /**
     * Checks and Returns True if Static Record Layout is set
     */
    fun isStaticRecordLayout(): Boolean {
        return this.optionalParameters.containsKey(RecordLOpt.StaticRecordLayout)
    }

    /**
     * Checks and Returns True if Static Address Offset it set
     */
    fun isStaticAddressOffsets(): Boolean {
        return this.optionalParameters.containsKey(RecordLOpt.StaticAddressOffsets)
    }

    fun getRelativePositions() {
        TODO()
    }
}