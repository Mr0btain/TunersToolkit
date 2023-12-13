package components.calibrationdata.a2l

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.data.A2lData
import components.calibrationdata.a2l.characteristic.readAsciiCharacteristicData
import components.calibrationdata.a2l.characteristic.readCurveCharacteristicData
import components.calibrationdata.a2l.characteristic.readMapCharacteristicData
import components.calibrationdata.a2l.characteristic.readValBlkCharacteristicData
import components.calibrationdata.a2l.characteristic.readValueCharacteristicData
import components.calibrationdata.classes.MapDataValues
import components.parsers.hex.HexSegmentData

fun getCharacteristicData(
    a2lData: A2lData,
    hexSegmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    return when (characteristic.type) {
        CharacteristicType.ASCII -> {
            readAsciiCharacteristicData(
                a2lData = a2lData,
                segmentData = hexSegmentData,
                characteristic = characteristic
            )
        }

        CharacteristicType.CURVE -> {
            readCurveCharacteristicData(
                a2lData = a2lData,
                segmentData = hexSegmentData,
                characteristic = characteristic
            )
        }

        CharacteristicType.MAP -> {
            readMapCharacteristicData(
                a2lData = a2lData,
                byteSegmentData = hexSegmentData,
                characteristic = characteristic
            )
        }

        CharacteristicType.CUBOID -> {
            // TODO Finish
            // readCuboidCharacteristicData(viewModel, characteristic)
            MapDataValues()
        }

        CharacteristicType.CUBE_4 -> {
            // TODO Finish
            // readCube4CharacteristicData(viewModel, characteristic)
            MapDataValues()
        }

        CharacteristicType.CUBE_5 -> {
            // TODO Finish
            // readCube5CharacteristicData(viewModel, characteristic)
            MapDataValues()
        }

        CharacteristicType.VAL_BLK -> {
            readValBlkCharacteristicData(
                a2lData = a2lData,
                segmentData = hexSegmentData,
                characteristic = characteristic
            )
        }

        CharacteristicType.VALUE -> {
            readValueCharacteristicData(
                a2lData = a2lData,
                segmentData = hexSegmentData,
                characteristic = characteristic
            )
        }

        CharacteristicType.NOT_SET -> {
            MapDataValues()
        }
    }
}