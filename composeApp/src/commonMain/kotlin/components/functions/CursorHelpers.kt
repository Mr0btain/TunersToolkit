package components.functions

import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun scrollToPos(pos: Int, scrollState: ScrollState) {
    GlobalScope.launch {
        scrollState.scrollTo(pos)
    }
}