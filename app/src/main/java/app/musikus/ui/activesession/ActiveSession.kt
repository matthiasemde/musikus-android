/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.ui.activesession

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.services.Actions
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString


const val FRACTION_HEIGHT_COLLAPSED = 0.3f
const val FRACTION_HEIGHT_EXTENDED = 0.7f

@Composable
fun ActiveSession(
    mainUiState: MainUiState,
    mainEventHandler: (event: MainUIEvent) -> Unit,
    deepLinkArgument: String?,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    timeProvider: TimeProvider,
    navigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent: (ActiveSessionUIEvent) -> Unit = viewModel::onEvent

    Scaffold (
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        content = {contentPadding ->
            if(uiState.isPaused) {
               PauseDialog(uiState, uiEvent)
            }

            Box(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1 - FRACTION_HEIGHT_COLLAPSED)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    // DEBUG
                    if (deepLinkArgument == Actions.FINISH.toString()) {
                        Text(
                            "Clicked on Finish in Notification",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Red
                        )
                    }

                    HeaderBar(uiState)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    PracticeTimer(uiState)
                    CurrentPracticingItem(uiState = uiState)
                    LibraryCard(
                        uiState = uiState.libraryUiState,
                        onLibraryItemClicked = { uiEvent(ActiveSessionUIEvent.StartNewSection(it)) },
                        onFolderClicked = {}
                    )

                }

                DraggableCard(uiState = uiState,
                    Modifier
                        .padding(
                            start = MaterialTheme.spacing.large,
                            end = MaterialTheme.spacing.large,
                        )
                        .align(Alignment.BottomCenter),

                )
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pager(
    uiState: ActiveSessionUiState,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        pageSpacing = MaterialTheme.spacing.medium,
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.large,
            end =  MaterialTheme.spacing.large,
            top = 40.dp
        )
    ) {page ->
        DraggableCard(
            uiState = uiState
        )

    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun getXAnchors(
    pageIndex: Int,
    screenDensity: Density,
    screenConfig: Configuration
) : DraggableAnchors<DragValueX> {
    with(screenDensity) {
        val scrollWidth = screenConfig.screenWidthDp.dp.toPx()
        return DraggableAnchors {
            DragValueX.Page1 at 0.dp.toPx()
            DragValueX.Page2 at scrollWidth * -1
            DragValueX.Page3 at scrollWidth * -2
            DragValueX.Page4 at scrollWidth * -3
        }
    }
}

enum class DragValueY { Start, End }
enum class DragValueX { Page1, Page2, Page3, Page4 }

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun DraggableCard(
    uiState: ActiveSessionUiState,
    modifier: Modifier = Modifier
) {
    val fractionOfHeightToMove = FRACTION_HEIGHT_EXTENDED - FRACTION_HEIGHT_COLLAPSED

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val yAnchors = with(density) {
        DraggableAnchors {
            val offset = (configuration.screenHeightDp * fractionOfHeightToMove).dp.toPx()
            DragValueY.Start at 0.dp.toPx()
            DragValueY.End at -offset
        }
    }
    val yState = remember { AnchoredDraggableState(
        initialValue = DragValueY.Start,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 100.dp.toPx() } },
        animationSpec = tween()
    ).apply {
        updateAnchors(yAnchors)
    } }

    val xState = remember {
        AnchoredDraggableState(
            initialValue = DragValueX.Page1,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween()
        ).apply {
            updateAnchors(getXAnchors(0, density, configuration))
        }
    }

    val initialHeight = (LocalConfiguration.current.screenHeightDp * FRACTION_HEIGHT_COLLAPSED).dp

    val lastY = remember { mutableStateOf(0f) }
    val movedFingerDown = remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .height(initialHeight - with(LocalDensity.current) {
                yState
                    .requireOffset()
                    .toDp()
            })
            .fillMaxWidth()
            .offset(x = 0.dp, y = 20.dp)
            .anchoredDraggable(yState, Orientation.Vertical)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    Log.d("TAG", "drag Gesture")
                    Log.d("TAG", "dragAmount: $dragAmount")
                    Log.d("TAG", "change: $change")
                }

                awaitEachGesture {

                    awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial)
                    // ACTION_DOWN here

                    do {

                        //This PointerEvent contains details including
                        // event, id, position and more
                        val event: PointerEvent = awaitPointerEvent(pass = PointerEventPass.Initial)


                        // ACTION_MOVE loop
                        Log.d("TAG", "event: ${event.changes.first()}")

//                        event.changes.forEach { it.consume() }

                    } while (event.changes.any { it.pressed })

                    // ACTION_UP is here

                }
            },
        shape = RoundedCornerShape(16.dp)
    ) {

        val listState = rememberLazyListState()


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .nestedScroll(
                    connection =  object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
//                            Log.d("TAG" , "delta: $delta")
                            return Offset.Zero
                        }
                        private fun isScroll() : Boolean {
                            return true
                        }
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {

                        awaitFirstDown()
                        // ACTION_DOWN here

                        do {

                            //This PointerEvent contains details including
                            // event, id, position and more
                            val event: PointerEvent = awaitPointerEvent()
                            // ACTION_MOVE loop



                            val y = event.changes.first().position.y
                            movedFingerDown.value = y > lastY.value
                            lastY.value = y
                            event.changes.forEach { it.consume() }
//                            Log.d("TAG", "event: ${event.changes.first()}")

//                            Log.d("TAG", "movedFingerDown: ${movedFingerDown.value}")


                        } while (event.changes.any { it.pressed })

                        // ACTION_UP is here


                    }
                }
            ,
            state = listState,
            userScrollEnabled = true//(!listState.canScrollBackward) || movedFingerDown.value

        ) {
            val toTopScrolled = !listState.canScrollBackward
            if (toTopScrolled) {
                Log.d("TAG", "reached end")
            }
            // Add your child elements here
            items(100) { index ->



//                scrollingDisabled.value =
//                    (yState.requireOffset() == 0f) ||                                   // card collapsed
//                    (movedFingerDown.value && listState.canScrollBackward)     // down moving finger && top of list

//                Log.d("TAG", "canScrollBackward: ${listState.canScrollBackward}")

//                Log.d("TAG", "scrollingDisabled: ${scrollingDisabled.value}"    )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Item $index"
                )
            }
        }
    }
}


/**
 * Returns whether the lazy list is currently scrolling up.
 */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

@Composable
private fun HeaderBar(
    uiState: ActiveSessionUiState
) {
    Text("<Header Placeholder>")
}


@Composable
private fun PracticeTimer(
    uiState: ActiveSessionUiState
) {
    Text(
        style = MaterialTheme.typography.displayMedium,
        text = getDurationString(uiState.totalSessionDuration, DurationFormat.HMS_DIGITAL).toString()
    )
    Text(text = "Practice time")
}


@Composable
private fun CurrentPracticingItem(
    uiState: ActiveSessionUiState
) {
    if (uiState.sections.isNotEmpty()) {
        val (name, duration) = uiState.sections.first()
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = getDurationString(
                    duration,
                    DurationFormat.HMS_DIGITAL
                ).toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.padding(MaterialTheme.spacing.small))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PauseDialog(
    uiState: ActiveSessionUiState,
    uiEvent: (ActiveSessionUIEvent) -> Unit
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = { }
    )
    {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text(
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.large),
                    text = "Pause: ${
                        getDurationString(
                            uiState.totalBreakDuration,
                            DurationFormat.HMS_DIGITAL
                        )
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(onClick = { uiEvent(ActiveSessionUIEvent.TogglePause) }) {
                Text("Resume")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryCard(
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
    onFolderClicked: (LibraryFolder) -> Unit,
) {
    FlowColumn(
        maxItemsInEachColumn = 3,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        // show a button for each library item
        for(libraryItem in (uiState.items + uiState.foldersWithItems.flatMap { it.items })) {
            Button(
                modifier = Modifier
                    .height(50.dp)
                    .width(200.dp),
                onClick = {
                    onLibraryItemClicked(libraryItem)
                }) {
                Text(text = libraryItem.name)
            }
        }
    }
}


@Composable
private fun SectionsList(
    uiState: ActiveSessionUiState,
    modifier: Modifier = Modifier
) {
    // Sections List
    if (uiState.sections.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            items(
                items = uiState.sections.drop(1)
            ) { (name, duration) ->

                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = name)

                    Text(text = getDurationString(duration, DurationFormat.HMS_DIGITAL).toString())
                }
            }
        }
    } else {
        Box (
            modifier = modifier
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Quotes",
            )
        }
    }
}

@Composable
private fun BottomRow(
    uiState: ActiveSessionUiState,
    onSaveClicked: () -> Unit
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ){
        Button(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures (
                    onDoubleTap = { Log.d("TAGH", "Double Tap") }
                )
            },
            onClick = {  }
        ) {
            Text(text = "Pause")
        }

        Button(onClick = onSaveClicked) {
            Text(text = "Save Session")
        }
    }
}
