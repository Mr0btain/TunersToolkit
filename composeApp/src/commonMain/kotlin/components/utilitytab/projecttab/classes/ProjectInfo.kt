package components.utilitytab.projecttab.classes

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(
    val name: String = "Blank",
    val projectVersion: String = "0",
    val allVersions: List<String> = listOf(),
    val ecuBrand: String = "",
    val ecuModel: String = "",
    val ecuVersion: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val engine: String = "",
    val transmission: String = ""
)
