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

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.Screen
import app.musikus.ui.activesession.metronome.MetronomeCardBody
import app.musikus.ui.activesession.metronome.MetronomeCardHeader
import app.musikus.ui.activesession.recorder.RecorderCardBody
import app.musikus.ui.activesession.recorder.RecorderCardHeader
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.components.DialogActions
import app.musikus.ui.components.DialogHeader
import app.musikus.ui.components.SwipeToDeleteContainer
import app.musikus.ui.components.fadingEdge
import app.musikus.ui.library.LibraryItemDialog
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.sessions.RatingBar
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText
import app.musikus.utils.getDurationString
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.time.Duration
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
    val icon: @Composable () -> Unit,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ActiveSession(
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    deepLinkArgument: String?,
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    val bottomSheetState = rememberBottomSheetScaffoldState()

    var showCardMetronome by rememberSaveable { mutableStateOf(false) }
    var metronomeCardIsExpanded by rememberSaveable { mutableStateOf(false) }
    var showCardRecord by rememberSaveable { mutableStateOf(false) }
    var showLibrary by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf(
        ToolsTab(
            title = "Metronome",
            icon = { Icon(imageVector = Icons.Outlined.Headset, contentDescription = null) },
            content = { MetronomeCard() }
        ),
        ToolsTab(
            title = "Recorder",
            icon = { Icon(imageVector = Icons.Outlined.Mic, contentDescription = null) },
            content = { RecorderCard() }
        )
    )

    BackHandler {
        if (showCardRecord || showCardMetronome) {
            showCardRecord = false
            showCardMetronome = false
            return@BackHandler
        }

        // notify the ViewModel that the back button was pressed
        eventHandler(ActiveSessionUiEvent.BackPressed)
        navigateUp()
    }



    Scaffold (
        topBar = {
            ActiveSessionTopBar(uiState = uiState, eventHandler = eventHandler, navigateUp =  navigateUp)
        },
        bottomBar = {
            ActiveSessionBottomTabs(tabs = tabs)
        },
    ) { paddingValues ->

        Surface(Modifier.padding(paddingValues)) {  // don't overlap with bottomBar

            BottomSheetScaffold(
                sheetContent = {
                    ToolsBottomSheet(
                        tabs = tabs,
                        sheetState = bottomSheetState,
                        onFabPress = { showLibrary = true }
                    )
                },
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                sheetTonalElevation = 0.dp,  // deprecated anyways
                sheetShadowElevation = 0.dp, // deprecated anyways
                scaffoldState = bottomSheetState,
                sheetPeekHeight = MaterialTheme.dimensions.toolsSheetPeekHeight,
                sheetDragHandle = { SheetDragHandle() }
            ) { sheetPadding ->

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(sheetPadding)) {

                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            PracticeTimer(
                                modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                                uiState = uiState
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                            CurrentPracticingItem(item = uiState.runningSection)
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                            SectionsList(
                                modifier = Modifier
                                    .padding(MaterialTheme.spacing.large)
                                    .align(Alignment.CenterHorizontally),
                                uiState = uiState,
                                onSectionDeleted = { section ->
                                    eventHandler(
                                        ActiveSessionUiEvent.DeleteSection(
                                            section.id
                                        )
                                    )
                                },
                                additionalBottomContentPadding =
                                MaterialTheme.spacing.large + 56.dp
                            )
                        }

                        Spacer(Modifier.weight(1f))
                    }
                    ExtendedFloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(MaterialTheme.spacing.large),
                        onClick = { showLibrary = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "New Library Item"
                            )
                        },
                        text = { Text("Add item") },
                        expanded = true
                    )
                }
            }  // BottomSheetScaffold
        }   // Surface
    }   // Scaffold


    /**
     * --------------------- Dialogs ---------------------
     */

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    if (showLibrary) {
        ModalBottomSheet(
            windowInsets = WindowInsets(top = 0.dp), // makes sure the scrim covers the status bar
            onDismissRequest = { showLibrary = false },
            sheetState = sheetState,
        ) {
            LibraryCardHeader(
                modifier = Modifier.height(MaterialTheme.dimensions.cardPeekHeight),
                uiState = uiState.libraryCardUiState.headerUiState,
                isCardCollapsed = false,
                eventHandler = eventHandler
            )
            HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.medium))
            LibraryCardBody(
                uiState = uiState.libraryCardUiState.bodyUiState,
                cardScrollState = rememberScrollState(),
                eventHandler = eventHandler,
                dismissEvent = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showLibrary = false
                        }
                    }

                }
            )
            Spacer(modifier = Modifier.height(400.dp))
        }
    }

//    if (uiState.isPaused) {
//        PauseDialog(
//            uiState.ongoingPauseDuration,
//            eventHandler
//        )
//    }

    uiState.addItemDialogUiState?.let { dialogUiState ->
        LibraryItemDialog(
            uiState = dialogUiState,
            eventHandler = { eventHandler(ActiveSessionUiEvent.ItemDialogUiEvent(it)) },
        )
    }

    val dialogUiState = uiState.dialogUiState

    dialogUiState.endDialogUiState?.let { endDialogUiState ->
        EndSessionDialog(
            rating = endDialogUiState.rating,
            comment = endDialogUiState.comment,
            onRatingChanged = { eventHandler(ActiveSessionUiEvent.EndDialogRatingChanged(it)) },
            onCommentChanged = {
                eventHandler(ActiveSessionUiEvent.EndDialogCommentChanged(it))
            },
            onDismiss = { eventHandler(ActiveSessionUiEvent.EndDialogDismissed) },
            onConfirm = {
                eventHandler(ActiveSessionUiEvent.EndDialogConfirmed)
                navigateUp()
            }
        )
    }

    if (dialogUiState.showDiscardSessionDialog) {
        DeleteConfirmationBottomSheet(
            confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
            confirmationText = UiText.DynamicString("Discard session?"),
            onDismiss = { eventHandler(ActiveSessionUiEvent.DiscardSessionDialogDismissed) },
            onConfirm = {
                eventHandler(ActiveSessionUiEvent.DiscardSessionDialogConfirmed)
                navigateUp()
            }
        )
    }

    if (deepLinkArgument == ActiveSessionActions.FINISH.name) {
        eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsBottomSheet(
    tabs: List<ToolsTab>,
    sheetState: BottomSheetScaffoldState,
    onFabPress: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxWidth()) {
        Column {
//            Box(
//                Modifier
//                    .fillMaxWidth()
//                    .background(MaterialTheme.colorScheme.surface)
//                    .height(28.dp)
//            )
//
//            Spacer(modifier = Modifier.height(28.dp))

            HorizontalPager(state = pagerState) { tabIndex ->
                ToolsCardLayout {
                    tabs[tabIndex].content()
                }
            }
        }
    }
}

@Composable
private fun ToolsTabRow(
    tabs: List<ToolsTab>,
    activeTabIndex: Int,
    onClick: (index: Int) -> Unit
) {
    TabRow(   // use ScrollableTabRow to achieve the smaller, left-aligned tab style
//        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        divider = {},
        indicator = { tabPositions ->
            if (activeTabIndex < tabPositions.size) {
                TabRowDefaults.PrimaryIndicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[activeTabIndex])
                        .offset(y = (-46).dp)
                        .height(2.dp),
                    width = 100.dp
                )
            }
        },
        selectedTabIndex = activeTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = activeTabIndex == index,
                    text = { Text(tab.title) },
                    onClick = { onClick(index) },
                )
            }
        }
}

@Composable
private fun ToolsCardLayout(
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
//                start = MaterialTheme.spacing.large,
//                end = MaterialTheme.spacing.large,
                bottom = MaterialTheme.spacing.medium,
            )
    ) {
        content()
    }
}

@Composable
private fun MetronomeCard() {
    MetronomeCardHeader(
        modifier = Modifier.height(MaterialTheme.dimensions.toolsHeaderHeight),
        onTextClicked = { }
    )
    MetronomeCardBody(
        modifier = Modifier.height(MaterialTheme.dimensions.toolsBodyHeight)
    )
}

@Composable
private fun RecorderCard() {
    RecorderCardHeader(modifier = Modifier.height(MaterialTheme.dimensions.toolsHeaderHeight))
    RecorderCardBody(modifier = Modifier.height(MaterialTheme.dimensions.toolsBodyHeight))
}

@Composable
private fun FeatureToggleButton(
    modifier: Modifier = Modifier,
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    if (active) {
        FilledTonalButton(
            onClick = onClick,
        ) {
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
        ) {
            Text(text = text)
        }

    }
}


@Composable
private fun ActiveSessionTopBar(
    uiState: ActiveSessionUiState,
    eventHandler: ActiveSessionUiEventHandler,
    navigateUp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row (Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
            IconButton(
                onClick = { navigateUp() }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            AnimatedVisibility(
                visible = uiState.runningSection != null,
                enter = slideInVertically(),
            ) {
                Row {
                    IconButton(
                        onClick = { eventHandler(ActiveSessionUiEvent.ShowDiscardSessionDialog) }
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    }

                    Column (Modifier.animateContentSize()) {
                        PauseButton(
                            onClick = { eventHandler(ActiveSessionUiEvent.TogglePause) },
                            paused = uiState.isPaused,
                            ongoingBreakDuration = uiState.ongoingPauseDuration
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = {
                            eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
                        }
                    ) {
                        Text(text = "Save Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionBottomTabs(
    tabs: List<ToolsTab>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider()
        val scope = rememberCoroutineScope()

        ToolsTabRow(
            tabs = tabs,
            activeTabIndex = 0, //pagerState.currentPage,
            onClick = { tabIndex ->
                scope.launch {
                    //                            pagerState.animateScrollToPage(tabIndex)
                }
                // don't wait for animation to finish
                scope.launch {
                    //                            if (tabIndex == pagerState.currentPage) {
                    //                                if (sheetState.bottomSheetState.currentValue != SheetValue.Expanded)
                    //                                    sheetState.bottomSheetState.expand()
                    //                                else
                    //                                    sheetState.bottomSheetState.partialExpand()
                    //                            }
                }
            }
        )
    }
}

@Preview
@Composable
private fun SheetDragHandle() {
    Surface(
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .padding(MaterialTheme.spacing.small)
            .size(
                width = 25.dp,
                height = 3.dp
            ),
        shape = RoundedCornerShape(50),
        content = { }
    )
}

@Composable
private fun PracticeTimer(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionUiState,
) {
    Text(
        modifier = modifier,
        style = if (!uiState.isPaused) {
            MaterialTheme.typography.displayLarge
        } else {
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        text = getDurationString(
            uiState.totalSessionDuration,
            DurationFormat.MS_DIGITAL
        ).toString(),
        fontWeight = FontWeight.Light,
        fontSize = 75.sp
    )
    Text(
        style = if (!uiState.isPaused) {
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.error)
        },
        text = if (!uiState.isPaused) "Practice time" else "Paused",
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentPracticingItem(
    item: ActiveSessionSectionListItemUiState?
) {

    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        if (item == null) return@AnimatedVisibility


        Column(
            Modifier
                .padding(horizontal = MaterialTheme.spacing.large)
                .clip(MaterialTheme.shapes.extraLarge)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, //uiState.runningSection?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.extraLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AnimatedContent(
                targetState = item.libraryItem.name,
                label = "currentPracticingItem",
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                }
            ) {
                Row(
                    modifier = Modifier
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // leading space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

//                Box(
//                    modifier = Modifier
//                        .padding(vertical = MaterialTheme.spacing.small)
//                        .size(16.dp)
//                        .clip(CircleShape)
//                        .background(item.color),
//                )
//
//                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = getDurationString(
                            item.duration,
                            DurationFormat.MS_DIGITAL
                        ).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // trailing space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionsList(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionUiState,
    additionalBottomContentPadding: Dp = 0.dp,
    onSectionDeleted: (ActiveSessionSectionListItemUiState) -> Unit = {},
) {
    if (uiState.runningSection == null) {
        Box(modifier = modifier) {
            Text(text = "Select a library item to start practicing")
        }
        return
    }

    val listState = rememberLazyListState()

    Column (modifier) {
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
                items = uiState.sections,
                key = { sectionElement -> sectionElement.id },
            ) { sectionElement ->
                SectionListElement(
                    modifier = Modifier.animateItemPlacement(),
                    shape = MaterialTheme.shapes.small,
                    sectionElement = sectionElement,
                    elementBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onSectionDeleted = onSectionDeleted,
                )
            }
        }
    }

    // scroll to top when new item is added
    var sectionLen by remember { mutableIntStateOf(uiState.sections.size) }
    LaunchedEffect(key1 = uiState.sections) {
        if (uiState.sections.size > sectionLen && listState.canScrollBackward) {
            listState.animateScrollToItem(0)
        }
        sectionLen = uiState.sections.size
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    elementBackgroundColor: Color,
    shape: Shape,
    sectionElement: ActiveSessionSectionListItemUiState,
    onSectionDeleted: (ActiveSessionSectionListItemUiState) -> Unit,
) {
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            deleted = targetValue == SwipeToDismissBoxValue.EndToStart
            deleted
        },
        positionalThreshold = with(LocalDensity.current) {
            { 112.dp.toPx() }
        }
    )
    SwipeToDeleteContainer(
        state = dismissState,
        deleted = deleted,
        onDeleted = { onSectionDeleted(sectionElement) }
    ) {
        Surface(
            // Surface for setting shape of item container
            modifier = modifier.height(56.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = shape
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(elementBackgroundColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // leading space
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                Box(
                    modifier = Modifier
                        .padding(vertical = MaterialTheme.spacing.small)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(sectionElement.color),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    modifier = Modifier.weight(1f),
                    text = sectionElement.libraryItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = getDurationString(
                        sectionElement.duration,
                        DurationFormat.MS_DIGITAL
                    ).toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
                // trailing space
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            }
        }
    }
}


@Composable
private fun PauseButton( onClick: () -> Unit, paused: Boolean, ongoingBreakDuration: Duration) {
    if(!paused) {
        IconButton(
            onClick = onClick
        ) {
            Icon(imageVector = Icons.Filled.Pause, contentDescription = null)
        }
    } else {
        ElevatedButton(
            onClick = onClick,
            colors = ButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f),
            )
        ) {
            Icon(imageVector = Icons.Outlined.PlayCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(text = "Pause: ${
                getDurationString(
                    ongoingBreakDuration,
                    DurationFormat.MS_DIGITAL
                )
            }",)
        }
    }
}

@Preview
@Composable
private fun PreviewSectionListElement() {
    SectionListElement(
        sectionElement = ActiveSessionSectionListItemUiState(
            id = UUIDConverter.deadBeef,
            color = Color.Red,
            libraryItem = LibraryItem(
                id = UUIDConverter.fromInt(1),
                name = "TestItem",
                colorIndex = 1,
                libraryFolderId = null,
                customOrder = null,
                createdAt = ZonedDateTime.now(),
                modifiedAt = ZonedDateTime.now()
            ),
            duration = 342.seconds
        ),
        elementBackgroundColor = MaterialTheme.colorScheme.surface,
        onSectionDeleted = {},
        shape = MaterialTheme.shapes.small
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionDraggableCardHeader(
    uiState: ActiveSessionDraggableCardHeaderUiState,
    cardState: DraggableCardLocalState,
    eventHandler: DraggableCardUiEventHandler,
) {
    when (uiState) {
        is ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState -> {
            LibraryCardHeader(
                uiState = uiState,
                isCardCollapsed = cardState.yState.currentValue == DragValueY.Collapsed,
                eventHandler = eventHandler
            )
        }

        is ActiveSessionDraggableCardHeaderUiState.RecorderCardHeaderUiState -> {
            RecorderCardHeader()
        }

        is ActiveSessionDraggableCardHeaderUiState.MetronomeCardHeaderUiState -> {
            MetronomeCardHeader(
                onTextClicked = { eventHandler(DraggableCardUiEvent.ResizeCard(DragValueY.Normal)) }
            )
        }
    }
}


@Composable
private fun ActiveSessionDraggableCardBody(
    uiState: ActiveSessionDraggableCardBodyUiState,
    cardState: DraggableCardLocalState,
    eventHandler: DraggableCardUiEventHandler,
) {
    when (uiState) {
        is ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState -> {
            LibraryCardBody(
                uiState = uiState,
                cardScrollState = cardState.scrollState,
                eventHandler = eventHandler,
            )
        }

        is ActiveSessionDraggableCardBodyUiState.RecorderCardBodyUiState -> {
            RecorderCardBody()
        }

        is ActiveSessionDraggableCardBodyUiState.MetronomeCardBodyUiState -> {
            MetronomeCardBody()
        }
    }
}

@Composable
private fun LibraryCardHeader(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState,
    isCardCollapsed: Boolean,
    eventHandler: ActiveSessionUiEventHandler,
) {
    // Header Folder List
    val state = rememberLazyListState()
    LazyRow(
        state = state,
        modifier = modifier
            .fadingEdge(state, vertical = false)
            .padding(vertical = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
    ) {

        items(uiState.folders) { folder ->
            LibraryFolderElement(
                folder = folder,
                onClick = {
                    eventHandler(ActiveSessionUiEvent.SelectFolder(folder?.id))
//                    if(isCardCollapsed) eventHandler(DraggableCardUiEvent.ResizeCard(DragValueY.Normal))
                },
                isSelected = folder?.id == uiState.selectedFolderId && !isCardCollapsed,
                showBadge = uiState.activeFolderId?.let { it.value == folder?.id } ?: false
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
    val color by animateColorAsState(
        targetValue =
        if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(0f)
        },
        label = "color",
        animationSpec = tween(200)
    )
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick(folder) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.small),
                badge = { if (showBadge) Badge() }
            ) {
                Icon(
                    Icons.Default.Folder,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = folder?.name ?: "no folder",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun LibraryCardBody(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState,
    cardScrollState: ScrollState,
    eventHandler: ActiveSessionUiEventHandler,
    dismissEvent: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fadingEdge(cardScrollState)
    ) {
        for ((item, lastPracticedDate) in uiState.itemsWithLastPracticedDate) {
            LibraryUiItem(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.spacing.small,
                    horizontal = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                ),
                item = item,
                lastPracticedDate = lastPracticedDate,
                selected = false,
                onShortClick = {
                    eventHandler(ActiveSessionUiEvent.SelectItem(item))
                    dismissEvent()
//                    eventHandler(DraggableCardUiEvent.ResizeCard(DragValueY.Normal))
                },
                onLongClick = { },
                compact = false,
                enabled = uiState.activeItemId != item.id
            )
        }
    }
}


@Composable
private fun PauseDialog(
    ongoingBreakDuration: Duration,
    eventHandler: ActiveSessionUiEventHandler,
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
                            ongoingBreakDuration,
                            DurationFormat.MS_DIGITAL
                        )
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(onClick = { eventHandler(ActiveSessionUiEvent.TogglePause) }) {
                Text("Resume")
            }
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