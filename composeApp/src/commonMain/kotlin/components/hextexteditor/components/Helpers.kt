package components.hextexteditor.components

fun getTextAlignmentLine(
    textWidth: Float,
    digitsPerLine: Int,
    bits: Int,
    totalWidth: Float
): Float {
    val lineWidth = ((textWidth * digitsPerLine) + (digitsPerLine / bits) * textWidth)
    return (totalWidth / 2) - (lineWidth / 2)
}