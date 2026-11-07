package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CharacteristicType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic.readAsciiCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic.readCurveCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic.readMapCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic.readValBlkCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic.readValueCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData

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