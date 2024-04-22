/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 *
 */

@file:OptIn(ExperimentalFoundationApi::class)

package app.musikus.ui.activesession

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.ColorSchemeSelections
import app.musikus.ui.Screen
import app.musikus.ui.activesession.metronome.MetronomeUi
import app.musikus.ui.activesession.recorder.RecorderUi
import app.musikus.ui.components.DialogActions
import app.musikus.ui.components.DialogHeader
import app.musikus.ui.components.SwipeToDeleteContainer
import app.musikus.ui.components.fadingEdge
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.sessions.RatingBar
import app.musikus.ui.theme.MusikusColorSchemeProvider
import app.musikus.ui.theme.MusikusPreviewElement1
import app.musikus.ui.theme.MusikusPreviewElement2
import app.musikus.ui.theme.MusikusPreviewElement3
import app.musikus.ui.theme.MusikusPreviewElement4
import app.musikus.ui.theme.MusikusPreviewElement5
import app.musikus.ui.theme.MusikusPreviewWholeScreen
import app.musikus.ui.theme.MusikusThemedPreview
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.libraryItemColors
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


const val CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN = 0.7f

/**
 * Actions that can be triggered by the Notification
 */
enum class ActiveSessionActions {
    OPEN, PAUSE, FINISH, METRONOME, RECORDER
}

data class ToolsTab(
    val title: String,
    val type: ActiveSessionTab,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActiveSession(
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    deepLinkArgument: String?,
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val bottomSheetState = rememberBottomSheetScaffoldState()

    // TODO move to somewhere final
    val tabs = persistentListOf(ToolsTab(type = ActiveSessionTab.METRONOME,
        title = "Metronome",
        content = { MetronomeUi() }),
        ToolsTab(type = ActiveSessionTab.RECORDER, title = "Recorder", content = { RecorderUi() })
    ).toImmutableList()

    ActiveSessionScreen(
        uiState = uiState,
        eventHandler = viewModel::onUiEvent,
        tabs = tabs,
        bottomSheetState = bottomSheetState,
    )

    /**
     * --------------------- Dialogs ---------------------
     */



//    val dialogUiState = uiState.dialogUiState
//
//    dialogUiState.endDialogUiState?.let { endDialogUiState ->
//        EndSessionDialog(
//            rating = endDialogUiState.rating,
//            comment = endDialogUiState.comment,
//            onRatingChanged = { eventHandler(ActiveSessionUiEvent.EndDialogRatingChanged(it)) },
//            onCommentChanged = {
//                eventHandler(ActiveSessionUiEvent.EndDialogCommentChanged(it))
//            },
//            onDismiss = { eventHandler(ActiveSessionUiEvent.EndDialogDismissed) },
//            onConfirm = {
//                eventHandler(ActiveSessionUiEvent.EndDialogConfirmed)
//                navigateUp()
//            }
//        )
//    }
//
//    if (dialogUiState.showDiscardSessionDialog) {
//        DeleteConfirmationBottomSheet(
//            confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
//            confirmationText = UiText.DynamicString("Discard session?"),
//            onDismiss = { eventHandler(ActiveSessionUiEvent.DiscardSessionDialogDismissed) },
//            onConfirm = {
//                eventHandler(ActiveSessionUiEvent.DiscardSessionDialogConfirmed)
//                navigateUp()
//            }
//        )
//    }
//
//    if (deepLinkArgument == ActiveSessionActions.FINISH.name) {
//        eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
//    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionScreen(
    uiState: ActiveSessionUiState,
    tabs: ImmutableList<ToolsTab>,
    eventHandler: (ActiveSessionUiEvent) -> Unit = {},
    bottomSheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    bottomSheetPagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
) {
    Scaffold(
        topBar = {
            ActiveSessionTopBar(
                uiState = uiState.topBarUiState,
                onDiscard = remember { { eventHandler(ActiveSessionUiEvent.ShowDiscardSessionDialog) } },
                onNavigateUp = remember { { eventHandler(ActiveSessionUiEvent.BackPressed) } },
                onTogglePause = remember { { eventHandler(ActiveSessionUiEvent.TogglePauseState) } },
                onSave = remember { { eventHandler(ActiveSessionUiEvent.ShowFinishDialog) } },
            )
        },
        bottomBar = {
            ActiveSessionBottomTabs(
                tabs = tabs,
                sheetState = bottomSheetState,
                pagerState = bottomSheetPagerState,
            )
        },
    ) { paddingValues ->
        Surface(Modifier.padding(paddingValues)) {  // don't overlap with bottomBar

            BottomSheetScaffold(sheetContent = {
                ActiveSessionToolsLayout(
                    tabs = tabs,
                    sheetState = bottomSheetState,
                    pagerState = bottomSheetPagerState
                )
            },
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                sheetTonalElevation = 0.dp,  // deprecated anyways
                sheetShadowElevation = 0.dp, // deprecated anyways
                sheetPeekHeight = MaterialTheme.dimensions.toolsSheetPeekHeight,
                scaffoldState = bottomSheetState,
                sheetDragHandle = { SheetDragHandle() },
                content = { sheetPadding ->
                    ActiveSessionMainContent(
                        contentPadding = sheetPadding,
                        uiState = uiState.mainContentUiState,
                        eventHandler = eventHandler
                    )
                })
        }
    }

    val scope = rememberCoroutineScope()

    // New Item Selector
    val sheetState = rememberModalBottomSheetState()
    if (uiState.newItemSelectorUiState.visible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),    // avoids jumping height when changing folders
            windowInsets = WindowInsets(top = 0.dp), // makes sure the scrim covers the status bar
            onDismissRequest = remember { { eventHandler(ActiveSessionUiEvent.ToggleNewItemSelectorVisible) } },
            sheetState = sheetState,
            shape = RectangleShape,
            dragHandle = {}
        ) {
            NewItemSelector(
                uiState = uiState.newItemSelectorUiState,
                onItemSelected = remember { { eventHandler(ActiveSessionUiEvent.SelectItem(it)) } },
                onClose = remember { {
                    scope.launch {
                        sheetState.hide()
                        eventHandler(ActiveSessionUiEvent.ToggleNewItemSelectorVisible)
                    }
                } },
            )
        }
    }

}


@Composable
private fun ActiveSessionMainContent(
    contentPadding: PaddingValues,
    uiState: MainContentUiState,
    eventHandler: (ActiveSessionUiEvent) -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {

        Column(Modifier.fillMaxWidth()) {

            Spacer(Modifier.weight(1f))

            Column(  // Animated container
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Big Timer
                PracticeTimer(
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                    uiState = uiState.timerUiState,
                    onResumeTimer = remember { { eventHandler(ActiveSessionUiEvent.TogglePauseState) } },
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                // Running Item
                CurrentPracticingItem(item = uiState.currentItemUiState)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                // Past Items
                SectionsList(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.large)
                        .align(Alignment.CenterHorizontally),
                    uiState = uiState.pastSectionsUiState,
                    onSectionDeleted = remember { { section ->
                        eventHandler(ActiveSessionUiEvent.DeleteSection(section.id))
                    } },
                    additionalBottomContentPadding = MaterialTheme.spacing.large + 56.dp    // 56.dp for FAB
                )
            }

            Spacer(Modifier.weight(1f))
        }

        // FAB for new Item
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(MaterialTheme.spacing.large),
            onClick = remember { { eventHandler(ActiveSessionUiEvent.ToggleNewItemSelectorVisible) } },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add, contentDescription = "New Library Item"
                )
            },
            text = { Text("Add item") },
            expanded = true
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionToolsLayout(
    tabs: ImmutableList<ToolsTab>,
    sheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    pagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
) {
    Box(Modifier.fillMaxWidth()) {
        Column {
            HorizontalPager(state = pagerState, userScrollEnabled = false) { tabIndex ->
                tabs[tabIndex].content()
            }
        }
    }
}


@Composable
private fun ActiveSessionTopBar(
    uiState: ActiveSessionTopBarUiState,
    onNavigateUp: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)   // take care of statusbar insets
            .padding(vertical = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
            IconButton(
                onClick = onNavigateUp
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            AnimatedVisibility(
                visible = uiState.visible,
                enter = slideInVertically(),
            ) {
                Row {

                    IconButton(
                        onClick = onDiscard
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    }

                    AnimatedVisibility(
                        visible = uiState.pauseButtonAppearance != SessionPausedResumedState.PAUSED,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        PauseButton(
                            onClick = onTogglePause,
                            paused = uiState.pauseButtonAppearance == SessionPausedResumedState.PAUSED,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = onSave
                    ) {
                        Text(text = "Save Session")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionBottomTabs(
    tabs: ImmutableList<ToolsTab>,
    sheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    pagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .windowInsetsPadding(WindowInsets.navigationBars)   // take care of navbar insets
    ) {

        HorizontalDivider()

        ToolsTabRow(tabs = tabs, activeTabIndex = pagerState.currentPage, onClick = { tabIndex ->
            scope.launch {
                // toggle expansion if on current page
                if (tabIndex == pagerState.currentPage) {
                    if (sheetState.bottomSheetState.currentValue != SheetValue.Expanded) sheetState.bottomSheetState.expand()
                    else sheetState.bottomSheetState.partialExpand()
                }
                pagerState.animateScrollToPage(tabIndex)
            }
        })
    }
}


@Composable
private fun ToolsTabRow(
    tabs: ImmutableList<ToolsTab>,
    activeTabIndex: Int,
    onClick: (index: Int) -> Unit,
) {
    TabRow(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        divider = {},
        indicator = { tabPositions ->
            if (activeTabIndex < tabPositions.size) {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[activeTabIndex])
                        .offset(y = (-45).dp), width = 100.dp, shape = RoundedCornerShape(
                        topStartPercent = 0,
                        topEndPercent = 0,
                        bottomStartPercent = 100,
                        bottomEndPercent = 100
                    )
                )
            }
        },
        selectedTabIndex = activeTabIndex
    ) {
        tabs.forEachIndexed { index, tab ->
            Row {
                Tab(selected = activeTabIndex == index,
                    text = { Text(tab.title) },
                    onClick = { onClick(index) })
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Surface(color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.small)
            .size(
                width = 25.dp, height = 3.dp
            ),
        shape = RoundedCornerShape(50),
        content = { })
}

@Composable
private fun PracticeTimer(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionTimerUiState,
    onResumeTimer: () -> Unit = {},
) {
    Column(
        Modifier.animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = MaterialTheme.typography.displayLarge,
            text = uiState.timerText,
            fontWeight = FontWeight.Light,
            fontSize = 75.sp
        )
        when (uiState.subHeadingAppearance) {
            SessionPausedResumedState.RUNNING -> Text(
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                text = uiState.subHeadingText,
            )

            SessionPausedResumedState.PAUSED -> ElevatedButton(
                onClick = onResumeTimer, colors = ButtonDefaults.elevatedButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(imageVector = Icons.Outlined.PlayCircle, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(text = uiState.subHeadingText)
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentPracticingItem(
    item: ActiveSessionCurrentItemUiState,
) {
    AnimatedVisibility(
        visible = item.visible,
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        if (!item.visible) return@AnimatedVisibility

        Surface(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large)
                .border(
                    width = 1.dp, color = item.color, shape = MaterialTheme.shapes.extraLarge
                ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            AnimatedContent(
                targetState = item.name,
                label = "currentPracticingItem",
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                }) { itemName ->
                Row(
                    modifier = Modifier.height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // leading space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))

                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = itemName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = item.durationText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // trailing space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionsList(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionCompletedSectionsUiState,
    additionalBottomContentPadding: Dp = 0.dp,
    onSectionDeleted: (CompletedSectionUiState) -> Unit = {},
) {
    if (!uiState.visible) {
        // TODO hide when first section started
        Box(modifier = modifier) {
            Text(text = "Select a library item to start practicing")
        }
        return
    }

    val listState = rememberLazyListState()

    Column(modifier) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            text = "Already practiced",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        LazyColumn(
            state = listState,
            modifier = Modifier
//                .fadingEdge(listState)    // TODO replace fadingEdge with sharp bottom corners
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            contentPadding = PaddingValues(
                bottom = additionalBottomContentPadding
            )
        ) {
            items(
                items = uiState.items,
                key = { item -> item.id },
            ) { item ->
                SectionListElement(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onSectionDeleted = onSectionDeleted,
                )
            }
        }
    }

    // scroll to top when new item is added
    var sectionLen by remember { mutableIntStateOf(uiState.items.size) }
    LaunchedEffect(key1 = uiState.items) {
        if (uiState.items.size > sectionLen && listState.canScrollBackward) {
            listState.animateScrollToItem(0)
        }
        sectionLen = uiState.items.size
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    item: CompletedSectionUiState,
    onSectionDeleted: (CompletedSectionUiState) -> Unit = {},
) {
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { targetValue ->
        deleted = targetValue == SwipeToDismissBoxValue.EndToStart
        deleted
    }, positionalThreshold = with(LocalDensity.current) {
        { 112.dp.toPx() }
    })
    SwipeToDeleteContainer(
        state = dismissState,
        deleted = deleted,
        onDeleted = { onSectionDeleted(item) }) {
        Surface(
            // Surface for setting shape of item container
            modifier = modifier.height(56.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // leading space
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))

                Box(
                    modifier = Modifier
                        .padding(vertical = MaterialTheme.spacing.small)
                        .width(10.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .clip(CircleShape)
                        .background(item.color),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = item.durationText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
                // trailing space
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
            }
        }
    }
}


@Composable
private fun PauseButton(
    onClick: () -> Unit,
    paused: Boolean,
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = if (!paused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null
        )
    }
}

@Composable
private fun NewItemSelector(
    modifier: Modifier = Modifier,
    uiState: NewItemSelectorUiState,
    onItemSelected: (LibraryItem) -> Unit,
    onClose: () -> Unit = {},
) {
    var selectedFolder: UUID? by remember { mutableStateOf(uiState.runningItem?.libraryFolderId) }
    Column(modifier.fillMaxWidth()) {

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row (
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = "Select a library item",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }

//        HorizontalDivider()
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        LibraryFoldersRow(
            folders = uiState.foldersWithItems.map { it.folder },
            highlightedFolderId = selectedFolder,
            folderWithBadge = uiState.runningItem?.libraryFolderId,
            onFolderSelected = remember {
                { folderId ->
                    selectedFolder = folderId
                }
            }
        )
//        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall))

        LibraryItemList(
            items = uiState.foldersWithItems.firstOrNull {
                it.folder.id == selectedFolder
            }?.items?.toImmutableList() ?: persistentListOf(),
            onItemClick = { libraryItem ->
                onItemSelected(libraryItem)
                onClose()
            },
            activeItemId = uiState.runningItem?.id
        )
    }
}

@Composable
private fun LibraryFoldersRow(
    modifier: Modifier = Modifier,
    folders: List<LibraryFolder>,
    highlightedFolderId: UUID?,
    folderWithBadge: UUID?,
    onFolderSelected: (UUID?) -> Unit,
) {
    val state = rememberLazyListState()
    LazyRow(
        state = state,
        modifier = modifier.fadingEdge(state, vertical = false),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),  // item spacing
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.extraLarge,
            end = MaterialTheme.spacing.medium
        ),
    ) {

        item(
            key = "noFolder",
            content = {
                LibraryFolderElement(
                    folder = null,
                    onClick = { onFolderSelected(null) },
                    isSelected = highlightedFolderId == null,
                    showBadge = folderWithBadge == null
                )
            }
        )

        items(folders) { folder ->
            LibraryFolderElement(
                folder = folder,
                onClick = { onFolderSelected(folder.id) },
                isSelected = folder.id == highlightedFolderId,
                showBadge = folder.id == folderWithBadge
            )
        }
    }
}


@Composable
private fun LibraryFolderElement(
    folder: LibraryFolder?,
    showBadge: Boolean = false,
    onClick: (LibraryFolder?) -> Unit,
    isSelected: Boolean,
) {
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }, label = "color", animationSpec = tween(200)
    )

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null,  // no ripple
                onClick = { onClick(folder) })
            .size(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
            badge = { if (showBadge) Badge() }) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder,
                tint = iconColor,
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            modifier = Modifier.basicMarquee(),
            text = folder?.name ?: "no folder",
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}


@Composable
private fun LibraryItemList(
    modifier: Modifier = Modifier,
    items: ImmutableList<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    activeItemId: UUID?,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = WindowInsets(
            top = MaterialTheme.spacing.medium,
        ).add(WindowInsets.navigationBars).asPaddingValues()    // don't get covered by navbars
    ) {
        items(items) {
            LibraryUiItem(
                item = it,
                lastPracticedDate = ZonedDateTime.now(),
                selected = false,
                onShortClick = { onItemClick(it) },
                onLongClick = { },
                enabled = activeItemId != it.id
            )
        }
    }
}


@Composable
fun EndSessionDialog(
    rating: Int,
    comment: String,
    onRatingChanged: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                DialogHeader(title = "Finish session")
                Column(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
                    Text(text = "Rate you session: ")
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RatingBar(
                            image = Icons.Default.Star,
                            rating = rating,
                            total = 5,
                            size = 36.dp,
                            onRatingChanged = onRatingChanged
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.medium))
                    OutlinedTextField(
                        value = comment,
                        placeholder = { Text("Comment (optional)") },
                        onValueChange = onCommentChanged
                    )
                }
                DialogActions(
                    dismissButtonText = "Keep Practicing",
                    confirmButtonText = "Save",
                    onDismissHandler = onDismiss,
                    onConfirmHandler = onConfirm
                )
            }
        }
    }
}


/**
 * ########################################### Previews ############################################
 */


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@MusikusPreviewWholeScreen
@Composable
private fun PreviewActiveSessionScreen(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {

    MusikusThemedPreview(theme) {

        ActiveSessionScreen(uiState = ActiveSessionUiState(
            topBarUiState = ActiveSessionTopBarUiState(
                visible = true, pauseButtonAppearance = SessionPausedResumedState.RUNNING
            ),
            mainContentUiState = MainContentUiState(
                timerUiState = ActiveSessionTimerUiState(
                    timerText = getDurationString(
                        (42 * 60 + 24).seconds, DurationFormat.MS_DIGITAL
                    ).toString(),
                    subHeadingText = "Practice Time",
                    subHeadingAppearance = SessionPausedResumedState.RUNNING
                ),
                currentItemUiState = dummyRunningItem,
                pastSectionsUiState = ActiveSessionCompletedSectionsUiState(
                    visible = true, items = dummySections.toList()
                ),
            ),
        ),
            tabs = listOf(ToolsTab(type = ActiveSessionTab.METRONOME,
                title = "Metronome",
                content = { }),
                ToolsTab(type = ActiveSessionTab.RECORDER, title = "Recorder", content = { })
            ).toImmutableList(),
        )
    }
}

@MusikusPreviewElement1
@Composable
private fun PreviewCurrentItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        CurrentPracticingItem(item = dummyRunningItem)
    }
}


@MusikusPreviewElement2
@Composable
private fun PreviewSectionItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        SectionListElement(item = dummySections.first())
    }
}


@MusikusPreviewElement3
@Composable
private fun PreviewLibraryRow(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        LibraryFoldersRow(
            folders = dummyFolders.toList(),
            highlightedFolderId = dummyFolders.first().id,
            folderWithBadge = dummyFolders.toList()[2].id,
            onFolderSelected = {}
        )
    }
}

@MusikusPreviewElement4
@Composable
private fun PreviewNewItemSelector(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        Column {
            NewItemSelector(
                uiState = NewItemSelectorUiState(
                    visible = true,
                    foldersWithItems = dummyFolders.map {
                        LibraryFolderWithItems(it, dummyLibraryItems.toList())
                    }.toList(),
                    runningItem = dummyLibraryItems.first().copy(
                        libraryFolderId = UUIDConverter.fromInt(1)
                    )
                ),
                onItemSelected = { }
            )
        }
    }
}

@MusikusPreviewElement5
@Composable
private fun PreviewLibraryItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        LibraryUiItem(
            item = dummyLibraryItems.first(),
            lastPracticedDate = ZonedDateTime.now(),
            selected = false,
            onShortClick = { /*TODO*/ },
            onLongClick = { /*TODO*/ })
    }
}


/** -------------------------------- Preview Parameter Providers -------------------------------- */


private val dummyRunningItem = ActiveSessionCurrentItemUiState(
    visible = true,
    color = libraryItemColors[0],
    name = LoremIpsum(Random.nextInt(4, 10)).values.first(),
    durationText = "32:19",
)


private val dummySections = (0..10).asSequence().map {
    CompletedSectionUiState(
        id = UUIDConverter.fromInt(it),
        name = LoremIpsum(Random.nextInt(1, 10)).values.first(),
        durationText = "12:32",
        color = libraryItemColors[it % libraryItemColors.size],
    )
}

private val dummyFolders = (0..10).asSequence().map {
    LibraryFolder(
        id = UUIDConverter.fromInt(it),
        customOrder = null,
        name = LoremIpsum(Random.nextInt(1, 5)).values.first(),
        modifiedAt = TimeProvider.uninitializedDateTime,
        createdAt = TimeProvider.uninitializedDateTime
    )
}

private val dummyLibraryItems = (1..20).asSequence().map {
    LibraryItem(
        id = UUIDConverter.fromInt(it),
        createdAt = TimeProvider.uninitializedDateTime,
        modifiedAt = TimeProvider.uninitializedDateTime,
        name = LoremIpsum(Random.nextInt(1, 10)).values.first(),
        colorIndex = it % libraryItemColors.size,
        customOrder = null,
        libraryFolderId = null
    )
}