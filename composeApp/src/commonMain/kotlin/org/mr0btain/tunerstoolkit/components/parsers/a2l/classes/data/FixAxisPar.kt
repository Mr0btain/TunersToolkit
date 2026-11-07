package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * For curves or maps, the axis points distribution is not
 * stored in memory, but it is computed on the basis of the
 * offset (initial value) and a difference. For the record
 * layouts used today, these parameters must be
 * included in the description file. The specification occurs
 * with keyword 'FIX_AXIS_PAR'
 */

@Serializable
data class FixAxisPar(
    val offset: Float = Float.NaN,
    val shift: Float = Float.NaN,
    val numberApo: UInt = UInt.MIN_VALUE
): A2lOptional