@file:Suppress("FunctionName")

package screens.editor

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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import components.a2l.classes.enums.CharacteristicType
import components.calibrationdata.classes.MapDataValues
import components.custom.persistedLazyScrollState
import components.functions.Keyboard
import components.functions.keyboardAsState
import components.helpers.ScreenSize
import components.hex.HexSegmentMap
import components.mappopup.GridWindow
import screens.editor.components.hexview.MainUtilityBar
import components.utilitytab.components.UtilityPagerView
import components.datastorage.ProjectFileType
import components.datastorage.ProjectFilesManager
import components.datastorage.processFileData
import components.formulaparser.FormulaParseDialog
import components.functions.filetools.saveNewProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import screens.editor.components.EditorView
import screens.editor.components.FloatingMapButton
import screens.editor.components.MainViewModel
import components.utilitytab.tabbar.CompactUtilityTabView
import ui.theme.TunerToolkitTheme

// TODO: Fix keyboard minimizing utility bar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CompactEditor(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val mTextSize = viewModel.textSize
    val mBitsPerLine = viewModel.digitsPerLine
    val mCoroutineScope = rememberCoroutineScope()
    val mSettings = viewModel.settings
    val mChunkList = viewModel.hexDataSegments[viewModel.selectedHexSegment.value]?.data?.toList() ?: listOf()
    val mMaps = viewModel.mapData
    val mIsAnimatedTabBarVisible = remember { mutableStateOf(true) }
    val mIsKeyboardUp = keyboardAsState()
    val mPagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 3 }
    val mColumnWidth = remember { mutableIntStateOf(0) }
    val mScrollState = rememberScrollState()
    val mFocusRequester = remember { FocusRequester() }
    val mLazyColumnState = persistedLazyScrollState(viewModel)
    var mOpenedMaps by remember { mutableStateOf(listOf<MapDataValues>()) }
    val mVisibleMaps = remember { mutableStateOf(listOf<String>()) }
    val mScreenSizeClass = calculateWindowSizeClass()
    val mFocusedMapPopUp = mutableIntStateOf(0)
    val mIsEmptySegmentsExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(
        key1 = viewModel.selectedHexSegment.value,
        key2 = mMaps.value.mapsSortedByChunk.size
    ) {
        mIsEmptySegmentsExpanded.value = mMaps.value.segmentId != viewModel.selectedHexSegment.value || mMaps.value.mapsSortedByChunk.isEmpty()
    }

    LaunchedEffect(key1 = mMaps.value.mapsSortedByChunk.size) {
        if (mMaps.value.mapsSortedByChunk.isNotEmpty()) {
            mOpenedMaps = mMaps.value.mapsSortedByChunk.values.first().subList(0, 4)
        }
    }

    // Needed to tell if we should hide the utility bar or map list
    // TODO: Needs fine tuned
    if (mSettings.value.hidePagerOnKeyUp) { mIsAnimatedTabBarVisible.value = mIsKeyboardUp.value != Keyboard.Opened }


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
                    selectedMaps = mOpenedMaps,
                    onClick = { map ->
                        mCoroutineScope.launch {
                            if (mVisibleMaps.value.contains(map.name)) {
                                // Remove our map from the list
                                val mNewList = mVisibleMaps.value.toMutableList()
                                mNewList.remove(map.name)
                                mVisibleMaps.value = mNewList
                            } else {
                                // Add our map to the list
                                mVisibleMaps.value = mVisibleMaps.value.plus(map.name)
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
                    .onSizeChanged { mColumnWidth.intValue = it.width },
            ) {
                // Check if user wants to display The Utility bar
                if (mSettings.value.showTopUtilityTabBar) {
                    AnimatedVisibility(
                        visible = mIsAnimatedTabBarVisible.value,
                        enter = expandTransition,
                        exit = collapseTransition,
                    ) {
                        CompactUtilityTabView(
                            pagerState = mPagerState,
                            scope = mCoroutineScope
                        )
                    }
                }

                // Using utility pager view to have multiple views and
                // this allows scrolling between views, use the state to change the
                // view displayed
                AnimatedVisibility(
                    visible = mIsAnimatedTabBarVisible.value,
                    enter = expandTransition,
                    exit = collapseTransition,
                ) {
                    Box(
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        UtilityPagerView(
                            modifier = Modifier.height(200.dp),
                            pagerState = mPagerState,
                            viewModel = viewModel,
                            scrollState = mScrollState,
                            scope = mCoroutineScope,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                background = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            focusRequester = mFocusRequester
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
                    scope = mCoroutineScope,
                    expanded = mIsEmptySegmentsExpanded
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
                        ScreenSize(viewModel, mScreenSizeClass.widthSizeClass)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp),
                            contentPadding = PaddingValues(0.dp),
                            state = mLazyColumnState
                        ) {
                            // For each item in the list, we create a section of the editor for it,
                            // This is needed for the larger files. will have to figure out how we
                            // can load maps into these sections, and possibly change the chunk size
                            // so maps don't overlap two chunks

                            itemsIndexed(mChunkList) { index, chunk ->
                                // Get maps for overlays if chunk contains it
                                val mMapDataInChunk by remember {
                                    mutableStateOf(
                                        if (mMaps.value.mapsSortedByChunk.containsKey(chunk.first)) {
                                            mMaps.value.mapsSortedByChunk[chunk.first] ?: listOf()
                                        } else listOf()
                                    )
                                }

                                if (mMapDataInChunk.isNotEmpty() || mIsEmptySegmentsExpanded.value) {
                                    // For each chunk make an EditorView
                                    EditorView(
                                        input = chunk,
                                        charsPerLine = mBitsPerLine.value,
                                        unitNumber = index,
                                        totalUnits = mChunkList.size,
                                        mapValues = mMapDataInChunk,
                                        textStyle = TextStyle(
                                            fontSize = mTextSize.value,
                                            fontFamily = viewModel.JetBrainsMono,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        ),
                                        viewModel = viewModel,
                                        focusRequester = mFocusRequester,
                                        coroutineScope = mCoroutineScope,
                                        chunkId = chunk.first
                                    )
                                }
                            }
                        }
                    }
                }
            }

            mOpenedMaps.forEachIndexed { index, mapDataValues ->
                GridWindow(
                    modifier = Modifier
                        .zIndex(if (mFocusedMapPopUp.value == index) 1f else 0f)
                        .focusable(enabled = true)
                        .focusRequester(mFocusRequester)
                        .onFocusChanged { mFocusedMapPopUp.value = index },
                    mapDataValues = mapDataValues,
                    axisTextSize = 14.sp,
                    dataTextSize =12.sp,
                    isVisible = mVisibleMaps,
                    onClickFocus = { mFocusedMapPopUp.value = index }
                )
            }
        }
    }
}