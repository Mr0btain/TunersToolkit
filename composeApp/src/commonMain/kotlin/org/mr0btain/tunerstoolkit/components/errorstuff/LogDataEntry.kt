package org.mr0btain.tunerstoolkit.components.errorstuff
data class LogDataEntry(
    val process: String = "",
    val message: String = "",
    val cause: String = "",
    val error: Boolean = false
)