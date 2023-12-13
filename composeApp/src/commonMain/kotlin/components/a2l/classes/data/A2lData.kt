package components.a2l.classes.data

import components.a2l.axis.AxisPts
import components.a2l.characteristic.Characteristic
import components.a2l.compumethod.CompuMethod
import components.a2l.computab.CompuTab
import components.a2l.compuvtab.CompuVTab
import components.a2l.compuvtabrange.CompuVTabRange
import components.a2l.functions.A2LFunction
import components.a2l.group.Group
import components.a2l.header.Header
import components.a2l.measurement.Measurement
import components.a2l.modcommon.ModCommon
import components.a2l.modpar.ModPar
import components.a2l.recordlayout.RecordLayout
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