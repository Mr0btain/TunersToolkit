@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.screens.editor
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import org.mr0btain.tunerstoolkit.components.screenconfig.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.custom.persistedLazyScrollState
import org.mr0btain.tunerstoolkit.components.datastorage.components.Keyboard
import org.mr0btain.tunerstoolkit.components.datastorage.components.keyboardAsState
import org.mr0btain.tunerstoolkit.components.helpers.ScreenSize
import org.mr0btain.tunerstoolkit.components.mappopup.GridWindow
import org.mr0btain.tunerstoolkit.screens.editor.components.hexview.MainUtilityBar
import org.mr0btain.tunerstoolkit.components.utilitytab.components.UtilityPagerView
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.screens.editor.components.EditorView
import org.mr0btain.tunerstoolkit.screens.editor.components.FloatingMapButton
import org.mr0btain.tunerstoolkit.MainViewModel
import org.mr0btain.tunerstoolkit.components.utilitytab.tabbar.CompactUtilityTabView
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme

// TODO: Fix keyboard minimizing utility bar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactEditor(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val textSize = viewModel.textSize
    val bitsPerLine = viewModel.digitsPerLine
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings
    val chunkList = viewModel.hexDataSegments[viewModel.selectedHexSegment.value]?.data?.toList() ?: listOf()
    val mapValuesSortedMutableState = viewModel.mapData
    val isAnimatedTabBarVisible = remember { mutableStateOf(true) }
    val isKeyboardUp = keyboardAsState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 3 }
    val columnWidth = remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val lazyColumnState = persistedLazyScrollState(viewModel)
    var openedMaps by remember { mutableStateOf(listOf<MapDataValues>()) }
    val visibleMaps = remember { mutableStateOf(listOf<String>()) }
    val screenSizeClass = calculateWindowSizeClass()
    val focusedMapPopUp = mutableIntStateOf(0)
    val isEmptySegmentsExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(
        key1 = viewModel.selectedHexSegment.value,
        key2 = mapValuesSortedMutableState.value.mapsSortedByChunk.size
    ) {
        isEmptySegmentsExpanded.value = mapValuesSortedMutableState.value.segmentId != viewModel.selectedHexSegment.value || mapValuesSortedMutableState.value.mapsSortedByChunk.isEmpty()
    }

    LaunchedEffect(key1 = mapValuesSortedMutableState.value.mapsSortedByChunk.size) {
        if (mapValuesSortedMutableState.value.mapsSortedByChunk.isNotEmpty()) {
            openedMaps = mapValuesSortedMutableState.value.mapsSortedByChunk.values.first().subList(0, 4)
        }
    }

    // Needed to tell if we should hide the utility bar or map list
    // TODO: Needs fine tuned
    if (settings.value.hidePagerOnKeyUp) { isAnimatedTabBarVisible.value = isKeyboardUp.value != Keyboard.Opened }


    // Opening Animation
    val expandTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }

    // Closing Animation
    val collapseTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }

    TunerToolkitTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingMapButton(
                    selectedMaps = openedMaps,
                    onClick = { map ->
                        scope.launch {
                            if (visibleMaps.value.contains(map.name)) {
                                // Remove our map from the list
                                val newList = visibleMaps.value.toMutableList()
                                newList.remove(map.name)
                                visibleMaps.value = newList
                            } else {
                                // Add our map to the list
                                visibleMaps.value = visibleMaps.value.plus(map.name)
                            }
                        }
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onSizeChanged { columnWidth.intValue = it.width },
            ) {
                // Check if user wants to display The Utility bar
                if (settings.value.showTopUtilityTabBar) {
                    AnimatedVisibility(
                        visible = isAnimatedTabBarVisible.value,
                        enter = expandTransition,
                        exit = collapseTransition,
                    ) {
                        CompactUtilityTabView(
                            pagerState = pagerState,
                            scope = scope
                        )
                    }
                }

                // Using utility pager view to have multiple views and
                // this allows scrolling between views, use the state to change the
                // view displayed
                AnimatedVisibility(
                    visible = isAnimatedTabBarVisible.value,
                    enter = expandTransition,
                    exit = collapseTransition,
                ) {
                    Box(
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        UtilityPagerView(
                            modifier = Modifier.height(200.dp),
                            pagerState = pagerState,
                            viewModel = viewModel,
                            scrollState = scrollState,
                            scope = scope,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                background = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            focusRequester = focusRequester
                        )
                    }
                }

                // Main utility bar that is always shown
                // TODO: Add functions and popup for actions
                MainUtilityBar(
                    addOnClick = {},
                    subOnClick = {},
                    percentOnClick = {},
                    viewModel = viewModel,
                    scope = scope,
                    expanded = isEmptySegmentsExpanded
                )

                Box(
                    modifier = Modifier
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxWidth()
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // This is to determine the size of the screen that we have to work with
                        // and update the text size based off the width
                        // TODO: Add ability to work with desktop width and landscape
                        ScreenSize(viewModel, screenSizeClass.widthSizeClass)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentPadding = PaddingValues(0.dp),
                            state = lazyColumnState
                        ) {
                            // For each item in the list, we create a section of the editor for it,
                            // This is needed for the larger files. will have to figure out how we
                            // can load maps into these sections, and possibly change the chunk size
                            // so maps don't overlap two chunks

                            itemsIndexed(chunkList) { index, chunk ->
                                // Get maps for overlays if chunk contains it
                                val mapDataInChunk by remember {
                                    mutableStateOf(
                                        if (mapValuesSortedMutableState.value.mapsSortedByChunk.containsKey(chunk.first)) {
                                            mapValuesSortedMutableState.value.mapsSortedByChunk[chunk.first] ?: listOf()
                                        } else listOf()
                                    )
                                }

                                if (mapDataInChunk.isNotEmpty() || isEmptySegmentsExpanded.value) {
                                    // For each chunk make an EditorView
                                    EditorView(
                                        input = chunk,
                                        charsPerLine = bitsPerLine.value,
                                        unitNumber = index,
                                        totalUnits = chunkList.size,
                                        mapValues = mapDataInChunk,
                                        textStyle = TextStyle(
                                            fontSize = textSize.value,
                                            fontFamily = viewModel.JetBrainsMono,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        ),
                                        viewModel = viewModel,
                                        focusRequester = focusRequester,
                                        coroutineScope = scope,
                                        chunkId = chunk.first
                                    )
                                }
                            }
                        }
                    }
                }
            }

            openedMaps.forEachIndexed { index, mapDataValues ->
                GridWindow(
                    modifier = Modifier
                        .zIndex(if (focusedMapPopUp.value == index) 1f else 0f)
                        .focusable(enabled = true)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusedMapPopUp.value = index },
                    mapDataValues = mapDataValues,
                    axisTextSize = 14.sp,
                    dataTextSize =12.sp,
                    isVisible = visibleMaps,
                    onClickFocus = { focusedMapPopUp.value = index }
                )
            }
        }
    }
}