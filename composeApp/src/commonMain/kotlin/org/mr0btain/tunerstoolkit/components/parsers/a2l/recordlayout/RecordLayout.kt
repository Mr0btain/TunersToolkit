package org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutAxisPoint
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutAxisRescale
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutIdentification
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutOffset
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutReserved
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentByte
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentFloat16
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentFloat32
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentFloat64
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentInt
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentLong
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AlignmentWord
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DistOp
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ExtendedLimits
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixNoAxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FncValues
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.NoAxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.NoRescale
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.RipAddress
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ShiftOp
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SrcAddress
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataType
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
)

fun RecordLayout.validate(): Boolean {
    return when {
        name.isBlank() -> false
        optionalParameters.isEmpty() -> false
        else -> true
    }
}

/**
 * Checks and Returns the Alignment Byte if set
 */
fun RecordLayout.getAlignmentByte(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentByte)) {
        val res = optionalParameters[RecordLOpt.AlignmentByte] as? AlignmentByte ?: AlignmentByte()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Float 16 if set
 */
fun RecordLayout.getAlignmentFloat16Ieee(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentFloat16Ieee)) {
        val res = optionalParameters[RecordLOpt.AlignmentFloat16Ieee] as? AlignmentFloat16 ?: AlignmentFloat16()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Float 32 if set
 */
fun RecordLayout.getAlignmentFloat32Ieee(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentFloat32Ieee)) {
        val res = optionalParameters[RecordLOpt.AlignmentFloat32Ieee] as? AlignmentFloat32 ?: AlignmentFloat32()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Float 64 if set
 */
fun RecordLayout.getAlignmentFloat64Ieee(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentFloat64Ieee)) {
        val res = optionalParameters[RecordLOpt.AlignmentFloat64Ieee] as? AlignmentFloat64 ?: AlignmentFloat64()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Int 64 if set
 */
fun RecordLayout.getAlignmentInt64(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentInt64)) {
        val res = optionalParameters[RecordLOpt.AlignmentInt64] as? AlignmentInt ?: AlignmentInt()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Long if set
 */
fun RecordLayout.getAlignmentLong(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentLong)) {
        val res = optionalParameters[RecordLOpt.AlignmentLong] as? AlignmentLong ?: AlignmentLong()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns the Alignment Word if set
 */
fun RecordLayout.getAlignmentWord(): Int {
    return if (optionalParameters.containsKey(RecordLOpt.AlignmentWord)) {
        val res = optionalParameters[RecordLOpt.AlignmentWord] as? AlignmentWord ?: AlignmentWord()
        res.num
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns AXIS_PTS_X if set
 */
fun RecordLayout.getAxisPtsX(): RecordLayoutAxisPoint {
    return if (this.optionalParameters.containsKey(RecordLOpt.AXIS_PTS_X)) {
        this.optionalParameters[RecordLOpt.AXIS_PTS_X] as? RecordLayoutAxisPoint
            ?: RecordLayoutAxisPoint()
    } else RecordLayoutAxisPoint()
}

/**
 * Checks and Returns AXIS_PTS_Y if set
 */
private fun RecordLayout.getAxisPtsY(): RecordLayoutAxisPoint {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_PTS_Y)) {
        optionalParameters[RecordLOpt.AXIS_PTS_Y] as? RecordLayoutAxisPoint
            ?: RecordLayoutAxisPoint()
    } else RecordLayoutAxisPoint()
}

/**
 * Checks and Returns AXIS_PTS_Z if set
 */
private fun RecordLayout.getAxisPtsZ(): RecordLayoutAxisPoint {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_PTS_Z)) {
        optionalParameters[RecordLOpt.AXIS_PTS_Z] as? RecordLayoutAxisPoint
            ?: RecordLayoutAxisPoint()
    } else RecordLayoutAxisPoint()
}

/**
 * Checks and Returns AXIS_PTS_4 if set
 */
private fun RecordLayout.getAxisPts4(): RecordLayoutAxisPoint {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_PTS_4)) {
        optionalParameters[RecordLOpt.AXIS_PTS_4] as? RecordLayoutAxisPoint
            ?: RecordLayoutAxisPoint()
    } else RecordLayoutAxisPoint()
}

/**
 * Checks and Returns AXIS_PTS_5 if set
 */
private fun RecordLayout.getAxisPts5(): RecordLayoutAxisPoint {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_PTS_5)) {
        optionalParameters[RecordLOpt.AXIS_PTS_5] as? RecordLayoutAxisPoint
            ?: RecordLayoutAxisPoint()
    } else RecordLayoutAxisPoint()
}

/**
 * Checks and Returns AXIS_RESCALE_X if set
 */
fun RecordLayout.getAxisRescaleX(): RecordLayoutAxisRescale {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_X)) {
        optionalParameters[RecordLOpt.AXIS_RESCALE_X] as? RecordLayoutAxisRescale
            ?: RecordLayoutAxisRescale()
    } else RecordLayoutAxisRescale()
}

/**
 * Checks and Returns AXIS_RESCALE_Y if set
 */
private fun RecordLayout.getAxisRescaleY(): RecordLayoutAxisRescale {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_Y)) {
        optionalParameters[RecordLOpt.AXIS_RESCALE_Y] as? RecordLayoutAxisRescale
            ?: RecordLayoutAxisRescale()
    } else RecordLayoutAxisRescale()
}

/**
 * Checks and Returns AXIS_RESCALE_Z if set
 */
private fun RecordLayout.getAxisRescaleZ(): RecordLayoutAxisRescale {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_Z)) {
        optionalParameters[RecordLOpt.AXIS_RESCALE_Z] as? RecordLayoutAxisRescale
            ?: RecordLayoutAxisRescale()
    } else RecordLayoutAxisRescale()
}

/**
 * Checks and Returns AXIS_RESCALE_4 if set
 */
private fun RecordLayout.getAxisRescale4(): RecordLayoutAxisRescale {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_4)) {
        optionalParameters[RecordLOpt.AXIS_RESCALE_4] as? RecordLayoutAxisRescale
            ?: RecordLayoutAxisRescale()
    } else RecordLayoutAxisRescale()
}

/**
 * Checks and Returns AXIS_RESCALE_5 if set
 */
private fun RecordLayout.getAxisRescale5(): RecordLayoutAxisRescale {
    return if (optionalParameters.containsKey(RecordLOpt.AXIS_RESCALE_5)) {
        optionalParameters[RecordLOpt.AXIS_RESCALE_5] as? RecordLayoutAxisRescale
            ?: RecordLayoutAxisRescale()
    } else RecordLayoutAxisRescale()
}

/**
 * Checks and Returns true if Calibration Access is set
 */
fun RecordLayout.getCalibrationAccess(): Boolean {
    return optionalParameters.containsKey(RecordLOpt.CalibrationAccess)
}

/**
 * Checks and Returns DIST_OP_X if set
 */
private fun RecordLayout.getDistOpX(): DistOp {
    return if (optionalParameters.containsKey(RecordLOpt.DIST_OP_X)) {
        optionalParameters[RecordLOpt.DIST_OP_X] as? DistOp ?: DistOp()
    } else DistOp()
}

/**
 * Checks and Returns DIST_OP_Y if set
 */
private fun RecordLayout.getDistOpY(): DistOp {
    return if (optionalParameters.containsKey(RecordLOpt.DIST_OP_Y)) {
        optionalParameters[RecordLOpt.DIST_OP_Y] as? DistOp ?: DistOp()
    } else DistOp()
}

/**
 * Checks and Returns DIST_OP_Z if set
 */
private fun RecordLayout.getDistOpZ(): DistOp {
    return if (optionalParameters.containsKey(RecordLOpt.DIST_OP_Z)) {
        optionalParameters[RecordLOpt.DIST_OP_Z] as? DistOp ?: DistOp()
    } else DistOp()
}

/**
 * Checks and Returns DIST_OP_4 if set
 */
private fun RecordLayout.getDistOp4(): DistOp {
    return if (optionalParameters.containsKey(RecordLOpt.DIST_OP_4)) {
        optionalParameters[RecordLOpt.DIST_OP_4] as? DistOp ?: DistOp()
    } else DistOp()
}

/**
 * Checks and Returns DIST_OP_5 if set
 */
private fun RecordLayout.getDistOp5(): DistOp {
    return if (optionalParameters.containsKey(RecordLOpt.DIST_OP_5)) {
        optionalParameters[RecordLOpt.DIST_OP_5] as? DistOp ?: DistOp()
    } else DistOp()
}

/**
 * Checks andReturns the Extended Limits if set
 */
fun RecordLayout.getExtendedLimits(): ExtendedLimits {
    return if (optionalParameters.containsKey(RecordLOpt.ExtendedLimits)) {
        optionalParameters[RecordLOpt.ExtendedLimits] as? ExtendedLimits
            ?: ExtendedLimits()
    } else ExtendedLimits()
}

/**
 * Checks and Returns FIX_NO_AXIS_X if set
 */
fun RecordLayout.getFixNoAxisX(): FixNoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_X)) {
        optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_X] as? FixNoAxisPts
            ?: FixNoAxisPts()
    } else FixNoAxisPts()
}

/**
 * Checks and Returns FIX_NO_AXIS_Y if set
 */
fun RecordLayout.getFixNoAxisY(): FixNoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_Y)) {
        optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_Y] as? FixNoAxisPts
            ?: FixNoAxisPts()
    } else FixNoAxisPts()
}

/**
 * Checks and Returns FIX_NO_AXIS_Z if set
 */
fun RecordLayout.getFixNoAxisZ(): FixNoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_Z)) {
        optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_Z] as? FixNoAxisPts
            ?: FixNoAxisPts()
    } else FixNoAxisPts()
}

/**
 * Checks and Returns FIX_NO_AXIS_4 if set
 */
fun RecordLayout.getFixNoAxis4(): FixNoAxisPts {
    return if (this.optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_4)) {
        this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_4] as? FixNoAxisPts
            ?: FixNoAxisPts()
    } else FixNoAxisPts()
}

/**
 * Checks and Returns FIX_NO_AXIS_5 if set
 */
fun RecordLayout.getFixNoAxis5(): FixNoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.FIX_NO_AXIS_PTS_5)) {
        this.optionalParameters[RecordLOpt.FIX_NO_AXIS_PTS_5] as? FixNoAxisPts
            ?: FixNoAxisPts()
    } else FixNoAxisPts()
}

/**
 * Checks and Returns the FNC Values if set
 */
fun RecordLayout.getFncValues(): FncValues {
    return if (optionalParameters.containsKey(RecordLOpt.FncValues)) {
        optionalParameters[RecordLOpt.FncValues] as? FncValues ?: FncValues()
    } else FncValues()
}

/**
 * Returns True if GuardRails is set
 */
fun RecordLayout.getGuardRails(): Boolean {
    return optionalParameters.containsKey(RecordLOpt.GuardRails)
}

/**
 * Checks and Returns Identification if set
 */
fun RecordLayout.getIdentification(): RecordLayoutIdentification {
    return if (optionalParameters.containsKey(RecordLOpt.Identification)) {
        optionalParameters[RecordLOpt.Identification] as? RecordLayoutIdentification
            ?: RecordLayoutIdentification()
    } else RecordLayoutIdentification()
}

/**
 * Checks and Returns NO_AXIS_PTS_X if set
 */
fun RecordLayout.getNoAxisPtsX(): NoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_X)) {
        optionalParameters[RecordLOpt.NO_AXIS_PTS_X] as? NoAxisPts ?: NoAxisPts()
    } else NoAxisPts()
}

/**
 * Checks and Returns NO_AXIS_PTS_Y if set
 */
fun RecordLayout.getNoAxisPtsY(): NoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_Y)) {
        optionalParameters[RecordLOpt.NO_AXIS_PTS_Y] as? NoAxisPts ?: NoAxisPts()
    } else NoAxisPts()
}

/**
 * Checks and Returns NO_AXIS_PTS_Z if set
 */
fun RecordLayout.getNoAxisPtsZ(): NoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_Z)) {
        optionalParameters[RecordLOpt.NO_AXIS_PTS_Z] as? NoAxisPts ?: NoAxisPts()
    } else NoAxisPts()
}

/**
 * Checks and Returns NO_AXIS_PTS_4 if set
 */
fun RecordLayout.getNoAxisPts4(): NoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_4)) {
        optionalParameters[RecordLOpt.NO_AXIS_PTS_4] as? NoAxisPts ?: NoAxisPts()
    } else NoAxisPts()
}

/**
 * Checks and Returns NO_AXIS_PTS_5 if set
 */
fun RecordLayout.getNoAxisPts5(): NoAxisPts {
    return if (optionalParameters.containsKey(RecordLOpt.NO_AXIS_PTS_5)) {
        optionalParameters[RecordLOpt.NO_AXIS_PTS_5] as? NoAxisPts ?: NoAxisPts()
    } else NoAxisPts()
}

/**
 * Checks and Returns NO_RESCALE_X if set
 */
private fun RecordLayout.getNoRescaleX(): NoRescale {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_X)) {
        optionalParameters[RecordLOpt.NO_RESCALE_X] as? NoRescale ?: NoRescale()
    } else NoRescale()
}

/**
 * Checks and Returns NO_RESCALE_Y if set
 */
private fun RecordLayout.getNoRescaleY(): NoRescale {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Y)) {
        optionalParameters[RecordLOpt.NO_RESCALE_Y] as? NoRescale ?: NoRescale()
    } else NoRescale()
}

/**
 * Checks and Returns NO_RESCALE_Z if set
 */
private fun RecordLayout.getNoRescaleZ(): NoRescale {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Z)) {
        optionalParameters[RecordLOpt.NO_RESCALE_Z] as? NoRescale ?: NoRescale()
    } else NoRescale()
}

/**
 * Checks and Returns NO_RESCALE_4 if set
 */
private fun RecordLayout.getNoRescale4(): NoRescale {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_4)) {
        optionalParameters[RecordLOpt.NO_RESCALE_4] as? NoRescale ?: NoRescale()
    } else NoRescale()
}

/**
 * Checks and Returns NO_RESCALE_5 if set
 */
private fun RecordLayout.getNoRescale5(): NoRescale {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_5)) {
        optionalParameters[RecordLOpt.NO_RESCALE_5] as? NoRescale ?: NoRescale()
    } else NoRescale()
}

/**
 * Checks and Returns OFFSET_X if set
 */
private fun RecordLayout.getOffsetX(): RecordLayoutOffset {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_X)) {
        optionalParameters[RecordLOpt.NO_RESCALE_X] as? RecordLayoutOffset
            ?: RecordLayoutOffset()
    } else RecordLayoutOffset()
}

/**
 * Checks and Returns OFFSET_Y if set
 */
private fun RecordLayout.getOffsetY(): RecordLayoutOffset {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Y)) {
        optionalParameters[RecordLOpt.NO_RESCALE_Y] as? RecordLayoutOffset
            ?: RecordLayoutOffset()
    } else RecordLayoutOffset()
}

/**
 * Checks and Returns OFFSET_Z if set
 */
private fun RecordLayout.getOffsetZ(): RecordLayoutOffset {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_Z)) {
        optionalParameters[RecordLOpt.NO_RESCALE_Z] as? RecordLayoutOffset
            ?: RecordLayoutOffset()
    } else RecordLayoutOffset()
}

/**
 * Checks and Returns OFFSET_4 if set
 */
private fun RecordLayout.getOffset4(): RecordLayoutOffset {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_4)) {
        optionalParameters[RecordLOpt.NO_RESCALE_4] as? RecordLayoutOffset
            ?: RecordLayoutOffset()
    } else RecordLayoutOffset()
}

/**
 * Checks and Returns OFFSET_5 if set
 */
private fun RecordLayout.getOffset5(): RecordLayoutOffset {
    return if (optionalParameters.containsKey(RecordLOpt.NO_RESCALE_5)) {
        optionalParameters[RecordLOpt.NO_RESCALE_5] as? RecordLayoutOffset
            ?: RecordLayoutOffset()
    } else RecordLayoutOffset()
}

/**
 * Checks and Returns the Reserved if set
 */
fun RecordLayout.getReserved(): RecordLayoutReserved {
    return if (optionalParameters.containsKey(RecordLOpt.Reserved)) {
        optionalParameters[RecordLOpt.Reserved] as? RecordLayoutReserved
            ?: RecordLayoutReserved()
    } else RecordLayoutReserved()
}

/**
 * Checks and Returns RIP_ADDR_W if set
 */
private fun RecordLayout.getRipAddrW(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_W)) {
        optionalParameters[RecordLOpt.RIP_ADDR_W] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and Returns RIP_ADDR_X if set
 */
private fun RecordLayout.getRipAddrX(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_X)) {
        optionalParameters[RecordLOpt.RIP_ADDR_X] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and Returns RIP_ADDR_Y if set
 */
private fun RecordLayout.getRipAddrY(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_Y)) {
        optionalParameters[RecordLOpt.RIP_ADDR_Y] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and Returns RIP_ADDR_Z if set
 */
private fun RecordLayout.getRipAddrZ(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_Z)) {
        optionalParameters[RecordLOpt.RIP_ADDR_Z] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and Returns RIP_ADDR_4 if set
 */
private fun RecordLayout.getRipAddr4(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_4)) {
        optionalParameters[RecordLOpt.RIP_ADDR_4] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and Returns RIP_ADDR_5 if set
 */
private fun RecordLayout.getRipAddr5(): RipAddress {
    return if (optionalParameters.containsKey(RecordLOpt.RIP_ADDR_5)) {
        optionalParameters[RecordLOpt.RIP_ADDR_5] as? RipAddress ?: RipAddress()
    } else RipAddress()
}

/**
 * Checks and returns an AxisPts if set
 */
fun RecordLayout.getAxisPts(type: AxisDataType): RecordLayoutAxisPoint {
    return when (type) {
        AxisDataType.X -> getAxisPtsX()
        AxisDataType.Y -> getAxisPtsY()
        AxisDataType.Z -> getAxisPtsZ()
        AxisDataType.Axis4 -> getAxisPts4()
        AxisDataType.Axis5 -> getAxisPts5()
        else -> RecordLayoutAxisPoint()
    }
}

/**
 * Checks and returns an AxisRescale if set
 */
fun RecordLayout.getAxisRescale(type: AxisDataType): RecordLayoutAxisRescale {
    return when (type) {
        AxisDataType.X -> getAxisRescaleX()
        AxisDataType.Y -> getAxisRescaleY()
        AxisDataType.Z -> getAxisRescaleZ()
        AxisDataType.Axis4 -> getAxisRescale4()
        AxisDataType.Axis5 -> getAxisRescale5()
        else -> RecordLayoutAxisRescale()
    }
}

/**
 * Checks and returns NoRescale if set
 */
fun RecordLayout.getNoRescale(type: AxisDataType): NoRescale {
    return when (type) {
        AxisDataType.X -> getNoRescaleX()
        AxisDataType.Y -> getNoRescaleY()
        AxisDataType.Z -> getNoRescaleZ()
        AxisDataType.Axis4 -> getNoRescale4()
        AxisDataType.Axis5 -> getNoRescale5()
        else -> NoRescale()
    }
}

/**
 * Checks and returns DistOp if set
 */
fun RecordLayout.getDistOp(type: AxisDataType): DistOp {
    return when (type) {
        AxisDataType.X -> getDistOpX()
        AxisDataType.Y -> getDistOpY()
        AxisDataType.Z -> getDistOpZ()
        AxisDataType.Axis4 -> getDistOp4()
        AxisDataType.Axis5 -> getDistOp5()
        else -> DistOp()
    }
}

/**
 * Checks and returns FixNoAxisPts if set
 */
fun RecordLayout.getFixNoAxisPts(type: AxisDataType): FixNoAxisPts {
    return when (type) {
        AxisDataType.X -> getFixNoAxisX()
        AxisDataType.Y -> getFixNoAxisY()
        AxisDataType.Z -> getFixNoAxisZ()
        AxisDataType.Axis4 -> getFixNoAxis4()
        AxisDataType.Axis5 -> getFixNoAxis5()
        else -> FixNoAxisPts()
    }
}

/**
 * Checks and returns NoAxisPts if set
 */
fun RecordLayout.getNoAxisPts(type: AxisDataType): NoAxisPts {
    return when (type) {
        AxisDataType.X -> getNoAxisPtsX()
        AxisDataType.Y -> getNoAxisPtsY()
        AxisDataType.Z -> getNoAxisPtsZ()
        AxisDataType.Axis4 -> getNoAxisPts4()
        AxisDataType.Axis5 -> getNoAxisPts5()
        else -> NoAxisPts()
    }
}

/**
 * Checks and returns Offset if set
 */
fun RecordLayout.getOffset(type: AxisDataType): RecordLayoutOffset {
    return when (type) {
        AxisDataType.X -> getOffsetX()
        AxisDataType.Y -> getOffsetY()
        AxisDataType.Z -> getOffsetZ()
        AxisDataType.Axis4 -> getOffset4()
        AxisDataType.Axis5 -> getOffset5()
        else -> RecordLayoutOffset()
    }
}

/**
 * Checks and returns RipAddr if set
 */
fun RecordLayout.getRipAddr(type: AxisDataType): RipAddress {
    return when (type) {
        AxisDataType.W -> getRipAddrW()
        AxisDataType.X -> getRipAddrX()
        AxisDataType.Y -> getRipAddrY()
        AxisDataType.Z -> getRipAddrZ()
        AxisDataType.Axis4 -> getRipAddr4()
        AxisDataType.Axis5 -> getRipAddr5()
        else -> RipAddress()
    }
}

/**
 * Checks and returns ShiftOp if set
 */
fun RecordLayout.getShiftOp(type: AxisDataType): ShiftOp {
    return when (type) {
        AxisDataType.X -> getShiftOpX()
        AxisDataType.Y -> getShiftOpY()
        AxisDataType.Z -> getShiftOpZ()
        AxisDataType.Axis4 -> getShiftOpX4()
        AxisDataType.Axis5 -> getShiftOp5()
        else -> ShiftOp()
    }
}

/**
 * Checks and returns SrcAddr if set
 */
fun RecordLayout.getSrcAddr(type: AxisDataType): SrcAddress {
    return when (type) {
        AxisDataType.X -> getSrcAddrX()
        AxisDataType.Y -> getSrcAddrY()
        AxisDataType.Z -> getSrcAddrZ()
        AxisDataType.Axis4 -> getSrcAddr4()
        AxisDataType.Axis5 -> getSrcAddr5()
        else -> SrcAddress()
    }
}

/**
 * Checks and Returns SRC_ADDR_X if set
 */
fun RecordLayout.getSrcAddrX(): SrcAddress {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_X)) {
        optionalParameters[RecordLOpt.SRC_ADDR_X] as? SrcAddress ?: SrcAddress()
    } else SrcAddress()
}

/**
 * Checks and Returns SRC_ADDR_Y if set
 */
fun RecordLayout.getSrcAddrY(): SrcAddress {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Y)) {
        optionalParameters[RecordLOpt.SRC_ADDR_Y] as? SrcAddress ?: SrcAddress()
    } else SrcAddress()
}

/**
 * Checks and Returns SRC_ADDR_Z if set
 */
fun RecordLayout.getSrcAddrZ(): SrcAddress {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Z)) {
        optionalParameters[RecordLOpt.SRC_ADDR_Z] as? SrcAddress ?: SrcAddress()
    } else SrcAddress()
}

/**
 * Checks and Returns SRC_ADDR_4 if set
 */
fun RecordLayout.getSrcAddr4(): SrcAddress {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_4)) {
        optionalParameters[RecordLOpt.SRC_ADDR_4] as? SrcAddress ?: SrcAddress()
    } else SrcAddress()
}

/**
 * Checks and Returns SRC_ADDR_5 if set
 */
fun RecordLayout.getSrcAddr5(): SrcAddress {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_5)) {
        optionalParameters[RecordLOpt.SRC_ADDR_5] as? SrcAddress ?: SrcAddress()
    } else SrcAddress()
}

/**
 * Checks and Returns SHIFT_OP_X if set
 */
fun RecordLayout.getShiftOpX(): ShiftOp {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_X)) {
        optionalParameters[RecordLOpt.SRC_ADDR_X] as? ShiftOp ?: ShiftOp()
    } else ShiftOp()
}

/**
 * Checks and Returns SHIFT_OP_Y if set
 */
private fun RecordLayout.getShiftOpY(): ShiftOp {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Y)) {
        optionalParameters[RecordLOpt.SRC_ADDR_Y] as? ShiftOp ?: ShiftOp()
    } else ShiftOp()
}

/**
 * Checks and Returns SHIFT_OP_Z if set
 */
private fun RecordLayout.getShiftOpZ(): ShiftOp {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_Z)) {
        optionalParameters[RecordLOpt.SRC_ADDR_Z] as? ShiftOp ?: ShiftOp()
    } else ShiftOp()
}

/**
 * Checks and Returns SHIFT_OP_4 if set
 */
private fun RecordLayout.getShiftOpX4(): ShiftOp {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_4)) {
        optionalParameters[RecordLOpt.SRC_ADDR_4] as? ShiftOp ?: ShiftOp()
    } else ShiftOp()
}

/**
 * Checks and Returns SHIFT_OP_5 if set
 */
private fun RecordLayout.getShiftOp5(): ShiftOp {
    return if (optionalParameters.containsKey(RecordLOpt.SRC_ADDR_5)) {
        optionalParameters[RecordLOpt.SRC_ADDR_5] as? ShiftOp ?: ShiftOp()
    } else ShiftOp()
}

/**
 * Checks and Returns True if Static Record Layout is set
 */
fun RecordLayout.isStaticRecordLayout(): Boolean {
    return optionalParameters.containsKey(RecordLOpt.StaticRecordLayout)
}

/**
 * Checks and Returns True if Static Address Offset it set
 */
fun RecordLayout.isStaticAddressOffsets(): Boolean {
    return optionalParameters.containsKey(RecordLOpt.StaticAddressOffsets)
}

fun RecordLayout.getRelativePositions() {
    TODO()
}