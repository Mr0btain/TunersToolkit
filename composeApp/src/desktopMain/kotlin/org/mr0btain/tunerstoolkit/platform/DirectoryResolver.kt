package org.mr0btain.tunerstoolkit.platform
import java.nio.file.FileSystems

/**
 * Base application directory resolver class
 */

abstract class DirectoryResolver {
    abstract fun getUserDataDir(appName: String, roaming: Boolean): String

    abstract fun getUserCacheDir(appName: String): String

    abstract fun getUserConfigDir(appName: String, roaming: Boolean): String

    abstract fun getUserLogDir(appName: String): String

    abstract fun getUserDownloadsDir(appName: String): String

    abstract fun getSharedDir(appName: String): String

    protected fun getUserHome(): String? { return System.getProperty("user.home") }

    protected fun buildPath(vararg elements: String?): String {
        val separator = FileSystems.getDefault().separator // Get the default separator for the system
        val buffer = StringBuilder()
        var lastElem = ""
        var tempElem: String

        for (element in elements) {
            // If it's null, just continue to next element
            if (element == null) { continue }

            // Check if the separator in the file path is the same as the system separator, if not swap it
            if (element.contains("/") && separator != "/") {
                tempElem = element.replace("/", separator)
            } else if (element.contains("\"") && separator != "/") {
                tempElem = element.replace("\"", separator)
            } else tempElem = element

            // Check if the last element has been set yet, if not then append the first element
            if (lastElem.isBlank()) {
                buffer.append(element)
            } else if (lastElem.endsWith(separator)) {
                // If the last element ends with a separator, check and remove the separator from the element were adding.
                buffer.append(if (tempElem.startsWith(separator)) tempElem.substring(1) else tempElem)
            } else {
                // Else if the last element does not end with a separator add one before adding the element
                if (!tempElem.startsWith(separator)) { buffer.append(separator) }

                buffer.append(tempElem)
            }

            // Set the last element
            lastElem = tempElem
        }
        return buffer.toString()
    }
}