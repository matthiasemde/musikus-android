/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.ui.activesession

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.services.Actions
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import kotlin.math.roundToInt


const val FRACTION_HEIGHT_COLLAPSED = 0.3f
const val FRACTION_HEIGHT_EXTENDED = 0.7f

enum class States {
    EXPANDED,
    COLLAPSED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    Scaffold  {contentPadding ->
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
                        .fillMaxHeight()
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

                Column(
                    Modifier.align(Alignment.BottomCenter)
                ) {
                    DraggableCard(
                        header = {CardHeader()},
                        body = {LibraryList(uiState = uiState.libraryUiState, onLibraryItemClicked = {} )}
                    )
                }

            }
        }
}


@Composable
private fun CardHeader() {
    Column (
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Heading",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(5.dp))
        HorizontalDivider()
    }
}


enum class DragValueY {
    Start,
    End
}

/**
 * DraggableCard BottomSheet-like implementation based on
 * https://www.droidcon.com/2021/11/09/how-to-master-swipeable-and-nestedscroll-modifiers-in-jetpack-compose/
 * and https://gist.github.com/arcadefire/7fe138c0ded1a36bee6dd57acdfa3d18, adapted to use
 * AnchoredDraggableState instead of SwipeableState
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggableCard(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit
) {

    val density = LocalDensity.current

    val yAnchors = with(density) {
        DraggableAnchors {
            DragValueY.Start at 0.dp.toPx()
            DragValueY.End at -300.dp.toPx()
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

    val scrollState = rememberScrollState()

    Log.d("TAG", "offset: ${yState.requireOffset().roundToInt()}")

    Box (
        Modifier
            .anchoredDraggable(yState, Orientation.Vertical)
            .height(with(density) {300.dp - yState.requireOffset().roundToInt().toDp()})
            .nestedScroll(
                object : NestedScrollConnection {

                    /**
                     * Lets the parent (this Box) consume gestures before the inner list scrolls.
                     *
                     * Relevant when "opening" the Bottom Sheet so that it swipes up instead of scrolling.
                     */
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y
                        return if (delta < 0) {
                            Offset(
                                x = 0f,
                                y = yState.dispatchRawDelta(delta)
                            )
                        } else {
                            Offset.Zero
                        }
                    }

                    /**
                     * Lets the parent (this Box) consume gestures after scrolling of the child is finished.
                     *
                     * Relevant for "closing" the Bottom Sheet so that it is dragged down when
                     * the list cannot be scrolled anymore.
                     */
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (source != NestedScrollSource.Drag) {
                            // We don't want to close the Bottom Sheet on "Fling" gestures,
                            // only when dragging
                            return Offset.Zero
                        }
                        val delta = available.y
                        return Offset(
                            x = 0f,
                            y = yState.dispatchRawDelta(delta)
                        )
                    }


                    /**
                     * Lets us actively invoke a "settle" (pull towards anchors) before a fling.
                     *
                     * Needed for getting pulled to the anchors on drag up when list is
                     * scrolled to the top.
                     */
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        return if (available.y < 0 && scrollState.value == 0) {
                            yState.settle(velocity = available.y)
                            available
                        } else {
                            Velocity.Zero
                        }
                    }


                    /**
                     * Lets us actively invoke a "settle" (pull towards anchors) after a fling of the
                     * scrollable child.
                     *
                     * Needed for closing the Bottom sheet completely when starting to "drag it down".
                     */
                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        yState.settle(velocity = available.y)
                        return super.onPostFling(consumed, available)
                    }
                }
            )
    ){
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                .fillMaxHeight()
        ) {

            header()

            Box(
                Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {

                body()
            }
        }
    }
}





@Composable
private fun HeaderBar(
    uiState: ActiveSessionUiState
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ){
        Button(
            onClick = {  }
        ) {
            Text(text = "Pause")
        }

        Button(onClick = {}) {
            Text(text = "Save Session")
        }
    }
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
        for(libraryItem in (uiState.rootItems + uiState.foldersWithItems.flatMap { it.items })) {
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
private fun LibraryList(
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
) {
    val activeFolder = remember { mutableStateOf(UUIDConverter.deadBeef) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Folder List
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(uiState.foldersWithItems) { folder ->
                Row {
                    Button(onClick = {
                        activeFolder.value = folder.folder.id
                    }) {
                        Text(folder.folder.name)
                    }
                }
            }
        }
        // Library Items
        Column {
            // show folder items or if folderId could not be found, show root Items
            val shownItems =
                uiState.foldersWithItems.find { it.folder.id == activeFolder.value }?.items
                    ?: uiState.rootItems
            for (item in shownItems) {
                LibraryUiItem(
                    modifier = Modifier.padding(
                        vertical = MaterialTheme.spacing.small,
                        horizontal = MaterialTheme.spacing.large
                    ),
                    item = item,
                    selected = false,
                    onShortClick = { /*TODO*/ },
                    onLongClick = { /*TODO*/ })
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