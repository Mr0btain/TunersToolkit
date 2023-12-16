package components.errorstuff

import androidx.compose.runtime.mutableStateListOf

class Logger {
    private val logData = mutableStateListOf<LogDataEntry>()

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
        }

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