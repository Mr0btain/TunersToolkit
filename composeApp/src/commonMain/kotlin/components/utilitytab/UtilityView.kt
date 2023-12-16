package components.utilitytab

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import components.custom.persistedPagerState
import components.utilitytab.components.ExpandedUtilityPager
import components.utilitytab.components.UtilityPagerView
import components.utilitytab.buttonbartab.ButtonBarTab
import components.utilitytab.maplisttab.MapsView
import components.utilitytab.tabbar.CompactUtilityTabView
import components.utilitytab.tabbar.ExpandedUtilityTabView
import getPlatform
import kotlinx.coroutines.CoroutineScope
import MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UtilityView(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scope: CoroutineScope,
    scrollState: ScrollState,
    windowHeightSizeClass: WindowHeightSizeClass,
    textStyle: TextStyle,
    focusRequester: FocusRequester
) {
    Column(
        modifier = modifier
    ) {
        if (windowHeightSizeClass == WindowHeightSizeClass.Compact || windowHeightSizeClass == WindowHeightSizeClass.Medium) {
            val mPagerState = persistedPagerState(viewModel)

            when (getPlatform().name) {
                "Java" -> {
                    Box(
                        modifier = modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        ExpandedUtilityTabView(
                            modifier = modifier.align(Alignment.Center),
                            viewModel = viewModel,
                            scope = scope
                        )
                    }

                    Spacer(modifier = Modifier.padding(2.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxHeight()
                    ) {
                        ExpandedUtilityPager(
                            modifier = modifier,
                            viewModel = viewModel,
                            scrollState = scrollState,
                            scope = scope,
                            textStyle = textStyle,
                            focusRequester = focusRequester
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        CompactUtilityTabView(
                            modifier = modifier.align(Alignment.Center),
                            pagerState = mPagerState,
                            scope = scope
                        )
                    }

                    Spacer(modifier = Modifier.padding(2.dp))

                    var mPagerBoxSize = IntSize(0,0)

                    Box(
                        modifier = modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .onGloballyPositioned { mPagerBoxSize = it.size }
                    ) {
                        UtilityPagerView(
                            modifier = modifier.height(200.dp),
                            pagerState = mPagerState,
                            viewModel = viewModel,
                            scrollState = scrollState,
                            scope = scope,
                            textStyle = textStyle,
                            focusRequester = focusRequester
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    ButtonBarTab(
                        mainViewModel = viewModel,
                        scrollState = scrollState,
                        scope = scope,
                        focusRequester = focusRequester
                    )
                }

                Spacer(modifier = Modifier.padding(2.dp))

                MapsView(
                    viewModel = viewModel,
                    scrollState = scrollState,
                    textStyle = textStyle
                )
            }
        }
    }
}