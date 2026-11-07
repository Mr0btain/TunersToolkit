package org.mr0btain.tunerstoolkit.components.mappopup
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density

class GridTextBoxParentData(
    val row: Int,
    val column: Int
): ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@GridTextBoxParentData
}