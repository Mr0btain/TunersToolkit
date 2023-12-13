package components.a2l.classes.data

import components.a2l.classes.enums.AddressType
import components.a2l.classes.enums.DataType
import components.a2l.classes.enums.IndexMode
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
data class FncValues(
    val position: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.Default,
    val indexMode: IndexMode = IndexMode.NOT_SET,
    val addressType: AddressType = AddressType.NOT_SET
): A2lOptional

/*
FNC_VALUES uint Position
    datatype Datatype
    enum IndexMode
    addrtype Addresstype

Parameters:
uint Position:   position of table values (function values) in the deposit
    structure (description of sequence of elements in the
    data record).
datatype DataType:  data type of the table values

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
addrtype Addresstype addressing of the table values (see enum addrtype).
Description:
Description of the table values (function values) of an adjustable object. If the
ALTERNATE option is used, the position parameter of values and axis-points indicates
their order. The concepts 'columns' and 'rows' relate to the XY coordinate system (see
also Appendix C).
For characteristic cuboids each XY plane is mapped as above. The cuboid is stored as
an array of maps with incremented or decremented Z coordinates. The CUBE_4 is
stored as an array of CUBOID with incremented or decremented Z2 coordinates. The
CUBE_5 is stored as an array of CUBE_4 with incremented or decremented Z3
coordinates. (Alternate not supported)
Example for ROW_DIR:
A 2 x 3 matrix M =
21 22 23
11 12 13
a a a
a a a
would be stored as follows: a11, a12, a13, a21,a22, a23
More generally, a matrix a i x j x k
would be listed as a111, … a11k, a121, … a1jk, a211, … aijk
Example for COLUMN_DIR:
A 2 x 3 matrix M =
21 22 23
11 12 13
a a a
a a a
would be stored as follows: a11, a21, a12, a22, a13, a23
More generally, a matrix a i x j x k
would be listed as a111, … ai11, a121, … aij1, a112, … aijk
Example:
FNC_VALUES 7
SWORD
COLUMN_DI
 */
