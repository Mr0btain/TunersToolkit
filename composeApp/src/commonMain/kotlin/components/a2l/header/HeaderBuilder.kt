package components.a2l.header

class HeaderBuilder {
    fun build(input: List<String>): Header {
        // No Need to check size as we look for identifiers on each line
        var version = ""
        var projNum = ""
        input.forEach {
            when {
                it.contains("VERSION") -> {
                    version = it.replace("VERSION", "")
                        .replace("\"", "").trim()
                }

                it.contains("PROJECT_NO") -> {
                    projNum = it.replace("PROJECT_NUM", "").trim()
                }
            }
        }
        return Header(
            version = version,
            projectNumber = projNum
        )
    }
}