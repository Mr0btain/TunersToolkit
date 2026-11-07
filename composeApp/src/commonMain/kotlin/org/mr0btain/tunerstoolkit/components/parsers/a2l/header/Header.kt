package org.mr0btain.tunerstoolkit.components.parsers.a2l.header
import kotlinx.serialization.Serializable

@Serializable
data class Header(
    val version: String = "",
    val projectNumber: String = ""
)