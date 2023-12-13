package components.maptextoverlay.components

import androidx.compose.ui.graphics.Path
import components.maptextoverlay.classes.PathData

fun PathData.toPath(): Path {
    val mResult = Path()
    // Set start position
    mResult.moveTo(this.start.x, this.start.y)
    // For each PathLine, add the line to the Path
    this.lines.forEach { line -> mResult.lineTo(line.x, line.y) }

    return mResult
}