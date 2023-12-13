package components.maptextoverlay.classes

import kotlinx.serialization.Serializable

@Serializable
data class PathData(val start: PathStart = PathStart(), val lines: List<PathLine> = listOf()) {
}

@Serializable
data class PathStart(val x: Float = 0f, val y: Float = 0f)

@Serializable
data class PathLine(val x: Float = 0f, val y: Float = 0f)