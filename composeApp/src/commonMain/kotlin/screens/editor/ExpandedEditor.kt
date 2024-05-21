package screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import components.calibrationdata.classes.MapDataValues
import components.custom.persistedLazyScrollState
import components.parsers.formula.FormulaParseDialog
import components.helpers.ScreenSize
import components.mappopup.GridWindow
import components.utilitytab.UtilityView
import screens.editor.components.EditorView
import screens.editor.components.FloatingMapButton
import MainViewModel
import screens.editor.components.hexview.MainUtilityBar
import ui.theme.TunerToolkitTheme

// TODO setup layout for this

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ExpandedEditor(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val textSize = viewModel.textSize
    val bitsPerLine = viewModel.digitsPerLine
    val coroutineScope = rememberCoroutineScope()
    val chunkList = viewModel.hexDataSegments[viewModel.selectedHexSegment.value]?.data?.toList() ?: listOf()
    val mapValuesSortedMutableState = viewModel.mapData
    val rowWidth = remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val lazyColumnState = persistedLazyScrollState(viewModel)
    var openedMaps by remember { mutableStateOf(listOf<MapDataValues>()) }
    val visibleMaps = remember { mutableStateOf(listOf<String>()) }
    val screenSizeClass1 = calculateWindowSizeClass()
    val focusedMapPopUp = mutableIntStateOf(0)
    var showEqDialog by remember { mutableStateOf(false) }
    val isEmptySegmentsExpanded = remember { mutableStateOf(false) }
    val listScrollState = rememberScrollState()

    LaunchedEffect(key1 = listScrollState.value) {
        println("Scroll state position: ${listScrollState.value}")
    }

    if (showEqDialog) {
        FormulaParseDialog(onDismissRequest = {showEqDialog = false})
    }

    LaunchedEffect(
        key1 = viewModel.selectedHexSegment.value,
        key2 = mapValuesSortedMutableState.value.mapsSortedByChunk.size
    ) {
        isEmptySegmentsExpanded.value = mapValuesSortedMutableState.value.segmentId != viewModel.selectedHexSegment.value || mapValuesSortedMutableState.value.mapsSortedByChunk.isEmpty()
    }

    LaunchedEffect(key1 = mapValuesSortedMutableState.value.mapsSortedByChunk.size) {
        if (mapValuesSortedMutableState.value.mapsSortedByChunk.isNotEmpty()) {
            openedMaps = mapValuesSortedMutableState.value.mapsSortedByChunk.values.elementAt(5).subList(0,4)
        }
    }

    TunerToolkitTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingMapButton(
                    selectedMaps = openedMaps,
                    onClick = { map ->
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
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onSizeChanged { rowWidth.intValue = it.width }
                    .padding(8.dp)
            ) {
                // Adaptable side bar
                UtilityView(
                    modifier = Modifier.width(350.dp),
                    viewModel = viewModel,
                    scope = coroutineScope,
                    scrollState = scrollState,
                    windowHeightSizeClass = screenSizeClass1.heightSizeClass,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        background = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    focusRequester = focusRequester
                )

                Spacer(modifier = Modifier.padding(4.dp))

                // Hex Editor
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .padding(4.dp),
                ) {
                    Box(modifier = Modifier.padding(4.dp).wrapContentSize()) {
                        // Main utility bar that is always shown
                        MainUtilityBar(
                            // Add to selection onclick action
                            addOnClick = { },
                            // Subtract from selection onclick action
                            subOnClick = {},
                            percentOnClick = {},
                            // View Model
                            viewModel = viewModel,
                            scope = coroutineScope,
                            expanded = isEmptySegmentsExpanded
                        )
                    }
                    val height = mutableIntStateOf(0)
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxHeight()
                            .onGloballyPositioned { height.value = it.size.height },
                        contentAlignment = Alignment.Center
                    ) {

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(6.dp)
                                ,
                            contentAlignment = Alignment.Center
                        ) {
                            // This is to determine the size of the screen that we have to work with
                            // and update the text size based off the width
                            // TODO: Add ability to work with desktop width and landscape
                            ScreenSize(viewModel, screenSizeClass1.widthSizeClass)
                            //val screenSizeClass = calculateWindowSizeClass()

                            LazyColumn(
                                modifier = Modifier,
                                state = lazyColumnState
                            ) {
                                // For each item in the list, we create a section of the editor for it,
                                // This is needed for the larger files. will have to figure out how we
                                // can load maps into these sections, and possibly change the chunk size
                                // so maps don't overlap two chunks

                                itemsIndexed(chunkList) { index, chunk ->
                                    // Get maps for overlays if chunk contains it
                                    val mapDataInChunk =
                                        if (mapValuesSortedMutableState.value.mapsSortedByChunk.containsKey(chunk.first)) {
                                            mapValuesSortedMutableState.value.mapsSortedByChunk[chunk.first] ?: listOf()
                                        } else listOf()
                                    //val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

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
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            viewModel = viewModel,
                                            focusRequester = focusRequester,
                                            coroutineScope = coroutineScope,
                                            chunkId = chunk.first
                                        )
                                    }
                                }
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