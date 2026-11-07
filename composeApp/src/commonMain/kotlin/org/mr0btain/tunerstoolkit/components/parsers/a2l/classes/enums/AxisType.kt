package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import kotlinx.serialization.Serializable

/**
 * Description of the axis points:
 *
 * @property CURVE_AXIS Curve axis. This axis type uses a
 * separate CURVE CHARACTERISTIC to
 * rescale the axis. The referenced
 * CURVE is used to look up an axis index,
 * and the index value is used by the
 * controller to determine the operating
 * point in the CURVE or MAP. See
 * Appendix D for more details.
 *
 * @property COM_AXIS Group axis points or description of the
 * axis points for deposit. For this variant of
 * the axis points the axis point values are
 * separated from the table values of the
 * curve or map in the emulation memory
 * and must be described by a special
 * AXIS_PTS data record. The reference to
 * this record occurs with the keyword
 * 'AXIS_PTS_REF'.
 *
 * @property FIX_AXIS This is a curve or a map with virtual axis
 * points that are not deposited at EPROM.
 * The axis points can be calculated from
 * parameters defined with keywords
 * FIX_AXIS_PAR, FIX_AXIS_PAR_DIST
 * and FIX_AXIS_PAR_LIST. The axis
 * points can‗t be modified.
 * Format of the description file
 * 46 ASAM MCD-2 MC Data Model for ECU Measurement and Calibration
 * Version 1.6.1
 *
 * @property RES_AXIS Rescale axis. For this variant of the axis
 * points the axis point values are
 * separated from the table values of the
 * curve or map in the emulation memory
 * and must be described by a special
 * AXIS_PTS data record. The reference to
 * this record occurs with the keyword
 * 'AXIS_PTS_REF'.
 * @property STD_AXIS Standard axis
 * @property NOT_SET Axis type not set
 */
@Serializable
enum class AxisType {
    STD_AXIS,
    COM_AXIS,
    FIX_AXIS,
    CURVE_AXIS,
    RES_AXIS,
    NOT_SET;
}

/**
 * Parses a String and determines the Axis Type if possible else
 * returns NOT_SET
 */

fun String.parseAxisType(): AxisType {
    return when (this.trim()) {
        "STD_AXIS" -> AxisType.STD_AXIS
        "COM_AXIS" -> AxisType.COM_AXIS
        "FIX_AXIS" -> AxisType.FIX_AXIS
        "CURVE_AXIS" -> AxisType.CURVE_AXIS
        "RES_AXIS" -> AxisType.RES_AXIS
        else -> AxisType.NOT_SET
    }
}
