package components.a2l.classes.enums

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class CalibrationAccess : A2lOptional {
    CALIBRATION,
    NO_CALIBRATION,
    NOT_IN_MCD_SYSTEM,
    OFFLINE_CALIBRATION,
    NOT_SET;
}

fun String.parseCalibrationAccess(): CalibrationAccess {
    return when (this.trim()) {
        "CALIBRATION" -> CalibrationAccess.CALIBRATION
        "NO_CALIBRATION" -> CalibrationAccess.NO_CALIBRATION
        "NOT_IN_MCD_SYSTEM" -> CalibrationAccess.NOT_IN_MCD_SYSTEM
        "OFFLINE_CALIBRATION" -> CalibrationAccess.OFFLINE_CALIBRATION
        else -> CalibrationAccess.CALIBRATION
    }
}