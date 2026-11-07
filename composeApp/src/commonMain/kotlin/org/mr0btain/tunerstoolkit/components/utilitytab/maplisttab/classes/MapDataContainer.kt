package org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab.classes
import kotlinx.serialization.Serializable

@Serializable
data class MapDataContainer(
    val title: String,
    val children: List<String>,
)

