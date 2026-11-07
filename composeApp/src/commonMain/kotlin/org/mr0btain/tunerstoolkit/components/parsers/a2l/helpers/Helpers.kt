package org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers
fun interpolateLinear(x1: Long, x2: Long, y1: Float, y2: Float, xi: Long): Double {
    return (y1 + (xi - x1) * ((y2 - y1) / (x2 - x1))).toDouble()
}