package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.CompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.computab.CompuTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtab.CompuVTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtabrange.CompuVTabRange
import org.mr0btain.tunerstoolkit.components.parsers.a2l.functions.A2LFunction
import org.mr0btain.tunerstoolkit.components.parsers.a2l.group.Group
import org.mr0btain.tunerstoolkit.components.parsers.a2l.header.Header
import org.mr0btain.tunerstoolkit.components.parsers.a2l.measurement.Measurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modcommon.ModCommon
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modpar.ModPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.RecordLayout
import kotlinx.serialization.Serializable

@Serializable
data class A2lData(
    val header: Header = Header(),
    val modPar: ModPar = ModPar(),
    val modCommon: ModCommon = ModCommon(),
    val characteristics: Map<String, Characteristic> = mapOf(),
    val compuMethods: Map<String, CompuMethod> = mapOf(),
    val compuTabs: Map<String, CompuTab> = mapOf(),
    val compuVTabs: Map<String, CompuVTab> = mapOf(),
    val compuVTabRanges: Map<String, CompuVTabRange> = mapOf(),
    val functions: Map<String, A2LFunction> = mapOf(),
    val groups: Map<String, Group> = mapOf(),
    val measurements: Map<String, Measurement> = mapOf(),
    val axisPoints: Map<String, AxisPts> = mapOf(),
    val recordLayouts: Map<String, RecordLayout> = mapOf()
)