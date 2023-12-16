@file:Suppress("FunctionName")

package components.utilitytab.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import components.utilitytab.buttonbartab.ButtonBarTab
import components.utilitytab.maplisttab.MapsView
import components.utilitytab.projecttab.ProjectInfoTab
import kotlinx.coroutines.CoroutineScope
import MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UtilityPagerView(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    viewModel: MainViewModel,
    scrollState: ScrollState,
    scope: CoroutineScope,
    textStyle: TextStyle,
    focusRequester: FocusRequester
) {
    // This allows scrolling between views, use the state to change the
    // view displayed

    Box(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        // content to be flipped through
        HorizontalPager(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            state = pagerState,
            pageContent = { page ->
                // content to be flipped through
                when (page) {
                    0 -> {
                        MapsView(
                            modifier = modifier,
                            viewModel = viewModel,
                            scrollState = scrollState,
                            textStyle = textStyle
                        )
                    }

                    1 -> {
                        ButtonBarTab(
                            viewModel,
                            scrollState = scrollState,
                            scope = scope,
                            focusRequester = focusRequester
                        )
                    }

                    2 -> {
                        ProjectInfoTab(
                            modifier = modifier,
                            viewModel = viewModel,
                            textStyle = textStyle
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun ExpandedUtilityPager(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scrollState: ScrollState,
    textStyle: TextStyle,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    Box(
        modifier = modifier
            .padding(8.dp).fillMaxSize()
    ) {
        // content to be flipped through
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .wrapContentHeight(),
        ) {
            // content to be flipped through
            when (viewModel.utilitySelection.value) {
                0 -> {
                    MapsView(
                        viewModel = viewModel,
                        scrollState = scrollState,
                        textStyle = textStyle
                    )
                }

                1 -> {
                    ButtonBarTab(
                        viewModel,
                        scrollState = scrollState,
                        scope = scope,
                        focusRequester = focusRequester
                    )
                }

                2 -> {
                    ProjectInfoTab(
                        textStyle = textStyle,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}