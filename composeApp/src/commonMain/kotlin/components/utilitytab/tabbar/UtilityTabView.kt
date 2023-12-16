@file:Suppress("FunctionName")

package components.utilitytab.tabbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactUtilityTabView(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    scope: CoroutineScope
) {
    Surface(
        shadowElevation = 4.dp,
        modifier = modifier.wrapContentSize()
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight(),
        ) {
            TabRow(
                modifier = modifier,
                selectedTabIndex = pagerState.currentPage
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        // Call scroll to on pagerState
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text(text = "Maps") }
                )

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        // Call scroll to on pagerState
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text(text = "Utilities") }
                )

                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        // Call scroll to on pagerState
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    text = { Text(text = "Project") }
                )
            }
        }
    }
}

@Composable
fun ExpandedUtilityTabView(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    Surface(
        shadowElevation = 4.dp,
        modifier = modifier.wrapContentSize()
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight(),
        ) {
            TabRow(
                modifier = modifier,
                selectedTabIndex = viewModel.utilitySelection.value
            ) {
                Tab(
                    selected = viewModel.utilitySelection.value == 0,
                    onClick = { scope.launch { viewModel.utilitySelection.value = 0 } },
                    text = { Text(text = "Maps") }
                )

                Tab(
                    selected = viewModel.utilitySelection.value == 1,
                    onClick = { scope.launch { viewModel.utilitySelection.value = 1 } },
                    text = { Text(text = "Utilities") }
                )

                Tab(
                    selected = viewModel.utilitySelection.value == 2,
                    onClick = { scope.launch { viewModel.utilitySelection.value = 2 } },
                    text = { Text(text = "Project") }
                )
            }
        }
    }
}