package components.parsers.a2l.classes.enums

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * For characteristic maps, this attribute is used to
 * describe how the 2-dimensional table values are
 * mapped onto the 1-dimensional address space
 */
@Serializable
enum class IndexMode : A2lOptional {
    ALTERNATE_CURVES,
    ALTERNATE_WITH_X,
    ALTERNATE_WITH_Y,
    ROW_DIR,
    COLUMN_DIR,
    NOT_SET;
}

/**
 * Parses a string and returns an IndexMode if one can be found, else returns None
 */
fun String.parseIndexMode(): IndexMode {
    when (this.trim()) {
        "ALTERNATE_CURVES" -> {
            return IndexMode.ALTERNATE_CURVES
        }
        "ALTERNATE_WITH_X" -> {
            return IndexMode.ALTERNATE_WITH_X
        }
        "ALTERNATE_WITH_Y" -> {
            return IndexMode.ALTERNATE_WITH_Y
        }
        "ROW_DIR" -> {
            return IndexMode.ROW_DIR
        }
        "COLUMN_DIR" -> {
            return IndexMode.COLUMN_DIR
        }
        else -> {
            return IndexMode.NOT_SET
        }
    }
}

/*
enum IndexMode: for characteristic maps, this attribute is used to
    describe how the 2-dimensional table values are
    mapped onto the 1-dimensional address space:

ALTERNATE_CURVES
curves: curves which share a common axis
are deposited in columns; each row
of memory contains values for all the
shared axis curves at a given axis
breakpoint. Required in order to
represent characteristics which
correspond to arrays of structures in
ECU program code. In the example
code below, DT10, DT20, etc are
treated as separate curves which
may have different conversions or
limits:-
maps: alternate curves not supported
cuboid: alternate curves not supported
cube_4: alternate curves not supported
cube_5: alternate curves not supported
typedef struct {
int DT10;
int DT20;
int DT30;
int DT40;
} VXP_TYPE;
const VXP_TYPE VX_PLUS_DELAY_TIMES[5] = {
{ 10, 3, 4, 8 },
{ 12, 2, 4, 6 },
{ 17, 9, 5, 8 },
{ 10, 1, 4, 8 },
{ 18, 3, 8, 8 },
};

ALTERNATE_WITH_X
maps: deposited in columns, the columns of
table values alternate with the
respective X-coordinates.
Format of the description file
112 ASAM MCD-2 MC Data Model for ECU Measurement and Calibration
Version 1.6.1
curves: table values and X-coordinate values
are deposited alternating.
cuboid: alternate with X not supported
cube_4: alternate with X not supported
cube_5: alternate with X not supported

ALTERNATE_WITH_Y
maps: deposited in rows, the rows of table
values alternate with the respective
Y-coordinates (maps only).
cuboid: alternate with Y not supported
cube_4: alternate with Y not supported
cube_5: alternate with Y not supported

COLUMN_DIR deposited in columns

ROW_DIR deposited in rows
 */