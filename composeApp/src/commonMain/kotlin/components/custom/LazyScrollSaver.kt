package components.custom

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import MainViewModel

@Composable
fun persistedLazyScrollState(viewModel: MainViewModel): LazyListState {
    val scrollState = rememberLazyListState(viewModel.firstVisibleItemIdx.value, viewModel.firstVisibleItemOffset.value)
    DisposableEffect(key1 = null) {
        onDispose {
            viewModel.firstVisibleItemIdx.value = scrollState.firstVisibleItemIndex
            viewModel.firstVisibleItemOffset.value = scrollState.firstVisibleItemScrollOffset
        }
    }
    return scrollState
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun persistedPagerState(viewModel: MainViewModel): PagerState {
    val mPagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 3 }
    DisposableEffect(key1 = null) {
        onDispose {
            viewModel.utilitySelection.value = mPagerState.currentPage
        }
    }

    return mPagerState
}