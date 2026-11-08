package org.mr0btain.tunerstoolkit.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.mr0btain.applogger.AppLogger
import org.mr0btain.applogger.LogBook
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmSwitchRow
import org.mr0btain.uikit.data.TmLogTerminal
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.theme.TmTheme

/** What the app has logged this session, as it's written */
@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val spacing = TmTheme.spacing
    val last by LogBook.last.collectAsState()
    val lines = remember(last) { LogBook.since(0).lines }
    var filter by remember { mutableStateOf("") }
    var debug by remember { mutableStateOf(AppLogger.debug) }

    Column(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TmPageHeader(
            title = "Log",
            onBack = null,
            subtitle = if (LogBook.storing) "This session, also saved to the app's log files" else "This session"
        )
        TmTextField(value = filter, onValueChange = { filter = it }, label = "Highlight", placeholder = "A word to pick out")
        TmLogTerminal(
            lines = lines,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            title = "tunerstoolkit",
            status = "${lines.size} lines",
            highlight = filter
        )
        TmSwitchRow(
            title = "Debug messages",
            subtitle = "Log the detail of what the app does, for tracking a problem down",
            checked = debug,
            onCheckedChange = {
                AppLogger.debug = it
                debug = it
            }
        )
    }
}
