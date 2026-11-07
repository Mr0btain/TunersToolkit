package org.mr0btain.tunerstoolkit.components.errorstuff
import androidx.compose.runtime.mutableStateListOf

class Logger {
    private val logData = mutableStateListOf<LogDataEntry>()

    // Add a message to the log
    fun addMessage(
        message: String = "",
        process: String = ""
        ) {
            logData.add(
                LogDataEntry(
                    message = message,
                    process = process
                )
            )

        // Print to the log
            println(
                LogDataEntry(
                    message = message,
                    process = process
                )
            )
        }

    // Add an error to the log
    fun addError(
        process: String,
        message: String,
        cause: String
    ) {
        logData.add(
            LogDataEntry(
                process = process,
                message = message,
                cause = cause,
                error = true
            )
        )
    }

    fun addLogItem(input: LogDataEntry) {
        logData.add(input)
    }

    fun addAll(input: List<LogDataEntry>) {
        logData.addAll(input)
    }

    fun log(): List<LogDataEntry> { return logData }
}