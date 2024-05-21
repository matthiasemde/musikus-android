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

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.components.DialogActions
import app.musikus.ui.components.DialogHeader
import app.musikus.ui.components.SwipeToDeleteContainer
import app.musikus.ui.components.conditional
import app.musikus.ui.components.fadingEdge
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.sessions.RatingBar
import app.musikus.ui.theme.MusikusColorSchemeProvider
import app.musikus.ui.theme.MusikusPreviewElement1
import app.musikus.ui.theme.MusikusPreviewElement2
import app.musikus.ui.theme.MusikusPreviewElement3
import app.musikus.ui.theme.MusikusPreviewElement4
import app.musikus.ui.theme.MusikusPreviewElement5
import app.musikus.ui.theme.MusikusPreviewElement6
import app.musikus.ui.theme.MusikusPreviewWholeScreen
import app.musikus.ui.theme.MusikusThemedPreview
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.libraryItemColors
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText
import app.musikus.utils.getDurationString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
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

data class ScreenSizeClass(
    val width: WindowWidthSizeClass,
    val height: WindowHeightSizeClass,
)

// https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes
// used for Previews only, always compare WindowWidthSizeClass and WindowHeightSizeClass separately
object ScreenSizeDefaults {

    val Phone = ScreenSizeClass(
        width = WindowWidthSizeClass.Compact,
        height = WindowHeightSizeClass.Expanded
    )

    val PhoneLandscape = ScreenSizeClass(
        width = WindowWidthSizeClass.Expanded,
        height = WindowHeightSizeClass.Compact
    )

    val Foldable = ScreenSizeClass(
        width = WindowWidthSizeClass.Medium,
        height = WindowHeightSizeClass.Medium
    )

    val LargeTablet = ScreenSizeClass(
        width = WindowWidthSizeClass.Expanded,
        height = WindowHeightSizeClass.Expanded
    )
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun ActiveSession(
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    deepLinkArgument: String?,
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val eventStates by viewModel.eventStates.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    val windowsSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)

    val snackbarHostState = remember { SnackbarHostState() }

    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            skipHiddenState = false,
        )
    )

    // TODO re-initialize bottomSheetState on configuration change
    // there is a bug where the bottom sheet will become fully collapsable after the screen has been
    // rotated. This can be fixed by re-initializing the bottomSheetState on configuration change.

    // successfully saved callback
    LaunchedEffect(eventStates.sessionSaved) {
        if (eventStates.sessionSaved) {
            // potentially show some goals progress or success message etc. in the future..
            navigateUp()
        }
    }

    // session discarded callback
    LaunchedEffect(eventStates.sessionDiscarded) {
        if (eventStates.sessionDiscarded) {
            navigateUp()
        }
    }


    // TODO move to somewhere final
    val tabs = persistentListOf(
        ToolsTab(
            type = ActiveSessionTab.METRONOME,
            title = "Metronome",
            content = { MetronomeUi() }
        ),
        ToolsTab(
            type = ActiveSessionTab.RECORDER,
            title = "Recorder",
            content = { RecorderUi(snackbarHostState = snackbarHostState) }
        )
    ).toImmutableList()
    // state for Tabs
    val bottomSheetPagerState = rememberPagerState(pageCount = { tabs.size })

    ActiveSessionScreen(
        uiState = uiState,
        eventHandler = eventHandler,
        navigateUp = navigateUp,
        tabs = tabs,
        bottomSheetState = bottomSheetState,
        bottomSheetPagerState = bottomSheetPagerState,
        snackbarHostState = snackbarHostState,
        sizeClass = ScreenSizeClass(
            windowsSizeClass.widthSizeClass,
            windowsSizeClass.heightSizeClass
        )
    )


    /** Handle deep link Arguments */

    val scope = rememberCoroutineScope()
    LaunchedEffect(deepLinkArgument) {
        when (deepLinkArgument) {
            ActiveSessionActions.METRONOME.name -> {
                // switch to metronome tab
                scope.launch {
                    bottomSheetPagerState.animateScrollToPage(
                        tabs.indexOfFirst { it.type == ActiveSessionTab.METRONOME }
                    )
                }
                // expand bottom sheet
                scope.launch {
                    bottomSheetState.bottomSheetState.expand()
                }
            }

            ActiveSessionActions.FINISH.name -> {
                eventHandler(ActiveSessionUiEvent.ToggleFinishDialog)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionScreen(
    uiState: State<ActiveSessionUiState>,
    tabs: ImmutableList<ToolsTab>,
    eventHandler: (ActiveSessionUiEvent) -> Unit = {},
    navigateUp: () -> Unit = {},
    bottomSheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    bottomSheetPagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
    sizeClass: ScreenSizeClass = ScreenSizeClass(
        WindowWidthSizeClass.Compact,
        WindowHeightSizeClass.Expanded
    ),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dialogVisibilities by uiState.value.dialogVisibilities.collectAsState()

    // Custom Scaffold for our elements which adapts to available window sizes
    ActiveSessionAdaptiveScaffold(
        screenSizeClass = sizeClass,
        snackbarHostState = snackbarHostState,
        bottomSheetState = bottomSheetState,
        topBar = {
            ActiveSessionTopBar(
                sessionState = uiState.value.sessionState.collectAsState(),
                onDiscard = remember { { eventHandler(ActiveSessionUiEvent.ToggleDiscardDialog) } },
                onNavigateUp = remember { { navigateUp() } },
                onTogglePause = remember { { eventHandler(ActiveSessionUiEvent.TogglePauseState) } },
                onSave = remember { { eventHandler(ActiveSessionUiEvent.ToggleFinishDialog) } },
            )
        },
        bottomBar = {
            ActiveSessionBottomTabs(
                tabs = tabs,
                sheetState = bottomSheetState,
                pagerState = bottomSheetPagerState,
                screenSizeClass = sizeClass,
            )
        },
        mainContent = { padding ->
            ActiveSessionMainContent(
                contentPadding = padding,
                uiState = uiState.value.mainContentUiState.collectAsState(),
                sessionState = uiState.value.sessionState.collectAsState(),
                snackbarHostState = snackbarHostState,
                eventHandler = eventHandler,
                screenSizeClass = sizeClass
            )
        },
        toolsContent = {
            ActiveSessionToolsLayout(
                tabs = tabs,
                sheetState = bottomSheetState,
                pagerState = bottomSheetPagerState
            )
        }
    )


    /**
     * --------------------- Dialogs ---------------------
     */


    /** New Item Selector */
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val newItemSelectorState = uiState.value.newItemSelectorUiState.collectAsState()
    if (dialogVisibilities.newItemSelectorVisible) {
        NewItemSelectorBottomSheet(
            uiState = newItemSelectorState,
            sheetState = sheetState,
            onItemSelected = remember {
                { item ->
                    eventHandler(ActiveSessionUiEvent.SelectItem(item))
                }
            },
            onNewItem = remember {
                {
                    eventHandler(ActiveSessionUiEvent.ToggleCreateItemDialog)
                }
            },
            onNewFolder = remember {
                {
                    eventHandler(ActiveSessionUiEvent.ToggleCreateFolderDialog)
                }
            },
            onDismissed = remember {
                {
                    eventHandler(ActiveSessionUiEvent.ToggleNewItemSelector)
                }
            }
        )
    }

    /** End Session Dialog */
    val dialogUiState = uiState.value.mainContentUiState.collectAsState()
    val endDialog = dialogUiState.value.endDialogUiState.collectAsState()
    if (dialogVisibilities.finishDialogVisible) {
        val dialogEvent = ActiveSessionUiEvent::EndDialogUiEvent

        EndSessionDialog(
            rating = endDialog.value.rating,
            comment = endDialog.value.comment,
            onDismiss = { eventHandler(ActiveSessionUiEvent.ToggleFinishDialog) },
            onRatingChanged = {
                eventHandler(
                    dialogEvent(ActiveSessionEndDialogUiEvent.RatingChanged(it))
                )
            },
            onCommentChanged = {
                eventHandler(
                    dialogEvent(ActiveSessionEndDialogUiEvent.CommentChanged(it))
                )
            },
            onConfirm = {
                eventHandler(
                    dialogEvent(ActiveSessionEndDialogUiEvent.Confirmed)
                )
            }
        )
    }

    /** Discard Session Dialog */
    if (dialogVisibilities.discardDialogVisible) {
        DeleteConfirmationBottomSheet(
            confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
            confirmationText = UiText.DynamicString("Discard session?"),
            onDismiss = { eventHandler(ActiveSessionUiEvent.ToggleDiscardDialog) },
            onConfirm = {
                eventHandler(ActiveSessionUiEvent.DiscardSessionDialogConfirmed)
                navigateUp()
            }
        )
    }
}


/**
 * Window-size adaptive layout for the ActiveSession screen.
 *
 * The screen is divided into two columns in landscape mode or small height.
 * The main content is on the left, the tools are on the right.
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSessionAdaptiveScaffold(
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    mainContent: @Composable (State<PaddingValues>) -> Unit,
    toolsContent: @Composable () -> Unit,
    bottomSheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    if (screenSizeClass.height == WindowHeightSizeClass.Compact) {

        /** Landscape / small height. Use two columns with main content left, bottom sheet right. */

        Scaffold(
            // Scaffold needed for topBar
            modifier = modifier,
            topBar = topBar,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) {
            Row(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                Column(     // left column
                    Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                ) {
                    mainContent(remember { mutableStateOf(PaddingValues(0.dp)) })
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                Column(     // right column
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Scaffold(   // Scaffold needed for bottomBar
                        bottomBar = bottomBar,
                        content = { paddingValues ->
                            Surface( // don't overlap with bottomBar
                                Modifier.padding(bottom = paddingValues.calculateBottomPadding())
                            ) {
                                ToolsBottomSheetScaffold(
                                    bottomSheetState = bottomSheetState,
                                    sheetContent = {
                                        toolsContent()
                                    },
                                    mainContent = { /* mainContent is in other column */ },
                                )
                            }
                        }
                    )
                }
            }
        }
    } else {

        /** Portrait. Use normal scaffold with bottom sheet */

        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { paddingValues ->
                Surface(Modifier.padding(paddingValues)) {  // don't overlap with bottomBar
                    ToolsBottomSheetScaffold(
                        bottomSheetState = bottomSheetState,
                        sheetContent = {
                            toolsContent()
                        },
                        mainContent = { sheetPadding ->
                            mainContent(sheetPadding)
                        },
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsBottomSheetScaffold(
    sheetContent: @Composable () -> Unit,
    mainContent: @Composable (State<PaddingValues>) -> Unit,
    bottomSheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState()
) {
    BottomSheetScaffold(
        sheetContent = { sheetContent() },
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetTonalElevation = 0.dp,  // deprecated anyways
        sheetShadowElevation = 0.dp, // deprecated anyways
        sheetPeekHeight = MaterialTheme.dimensions.toolsSheetPeekHeight,
        scaffoldState = bottomSheetState,
        sheetDragHandle = { SheetDragHandle() },
        content = { sheetPadding ->
            // sheet
            val bottomPadding = animateDpAsState(
                if (bottomSheetState.bottomSheetState.currentValue != SheetValue.Hidden) {
                    sheetPadding.calculateBottomPadding()
                } else {
                    0.dp
                }, label = ""
            )
            val paddingValues = remember {
                derivedStateOf {
                    PaddingValues(
                        top = sheetPadding.calculateTopPadding(),
                        // TODO maybe get native LayoutDirection?
                        start = sheetPadding.calculateStartPadding(layoutDirection = LayoutDirection.Ltr),
                        end = sheetPadding.calculateEndPadding(layoutDirection = LayoutDirection.Ltr),
                        bottom = bottomPadding.value
                    )
                }
            }
            mainContent(paddingValues)
        }
    )
}


@Composable
private fun ActiveSessionMainContent(
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass = ScreenSizeDefaults.Phone,
    contentPadding: State<PaddingValues>,
    uiState: State<ActiveSessionContentUiState>,
    sessionState: State<ActiveSessionState>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    eventHandler: (ActiveSessionUiEvent) -> Unit = {},
) {
    // condense UI a bit if there is limited space
    val limitedHeight = screenSizeClass.height == WindowHeightSizeClass.Compact

    var addSectionFABVisible by remember { mutableStateOf(true) }
    val sectionsListState = rememberLazyListState()
    // Nested scroll for control FAB
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Hide FAB when user scrolls and there are actually enough elements to scroll
                if (available.y < -1 && sectionsListState.canScrollForward) {
                    addSectionFABVisible = false
                }

                // Show FAB
                if (available.y > 1) {
                    addSectionFABVisible = true
                }

                return Offset.Zero
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .padding(contentPadding.value)
    ) {

        Column(
            Modifier
                .widthIn(min = 0.dp, max = 600.dp) // limit width for large screens
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {

            if (!limitedHeight) Spacer(Modifier.weight(1f))

            Column(  // Animated container
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Big Timer
                PracticeTimer(
                    uiState = uiState.value.timerUiState.collectAsState(),
                    sessionState = sessionState,
                    screenSizeClass = screenSizeClass,
                    onResumeTimer = remember { { eventHandler(ActiveSessionUiEvent.TogglePauseState) } },
                )

                if (limitedHeight)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                else
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                // Running Item
                CurrentPracticingItem(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
                    screenSizeClass = screenSizeClass,
                    uiState = uiState.value.currentItemUiState.collectAsState(),
                )

                if (limitedHeight)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                else
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                // Past Items
                val pastItemsState = uiState.value.pastSectionsUiState.collectAsState()
                if (pastItemsState.value != null) {
                    SectionsList(
                        uiState = pastItemsState,
                        nestedScrollConnection = nestedScrollConnection,    // for hiding the FAB
                        listState = sectionsListState,
                        snackbarHostState = snackbarHostState,
                        onSectionDeleted = remember {
                            { section ->
                                eventHandler(ActiveSessionUiEvent.DeleteSection(section.id))
                            }
                        },
                        additionalBottomContentPadding =
                        // 56.dp for FAB, landscape FAB is hidden, so no content padding needed
                        MaterialTheme.spacing.large + if (!limitedHeight) 56.dp else 0.dp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }

        AddSectionFAB(
            isVisible = addSectionFABVisible || !limitedHeight,    // only hide FAB in landscape layout
            sessionState = sessionState,
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = remember { { eventHandler(ActiveSessionUiEvent.ToggleNewItemSelector) } }
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewItemSelectorBottomSheet(
    uiState: State<NewItemSelectorUiState>,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onItemSelected: (LibraryItem) -> Unit = {},
    onNewItem: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    onDismissed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxHeight(0.6f),    // avoid jumping height when changing folders
        windowInsets = WindowInsets(top = 0.dp), // makes sure the scrim covers the status bar
        onDismissRequest = remember { onDismissed },
        sheetState = sheetState,
        shape = RectangleShape,
        dragHandle = {},
    ) {
        NewItemSelector(
            uiState = uiState,
            onItemSelected = onItemSelected,
            onNewItem = onNewItem,
            onNewFolder = onNewFolder,
            onClose = remember {
                {
                    scope.launch {
                        sheetState.hide()
                        onDismissed()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionToolsLayout(
    modifier: Modifier = Modifier,
    tabs: ImmutableList<ToolsTab>,
    sheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(), // TODO remove?
    pagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
) {
    Box(modifier.fillMaxWidth()) {
        Column {
            HorizontalPager(state = pagerState, userScrollEnabled = false) { tabIndex ->
                tabs[tabIndex].content()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSessionTopBar(
    sessionState: State<ActiveSessionState>,
    onNavigateUp: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onNavigateUp, content = {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            })
        },
        actions = {
            AnimatedVisibility(
                visible = sessionState.value == ActiveSessionState.RUNNING
                        || sessionState.value == ActiveSessionState.PAUSED,
                enter = slideInVertically(),
            ) {
                Row {
                    AnimatedVisibility(
                        visible = sessionState.value == ActiveSessionState.RUNNING,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        PauseButton(onClick = onTogglePause)
                    }

                    IconButton(
                        onClick = onDiscard,
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    }

                    TextButton(
                        onClick = onSave
                    ) {
                        Text(text = "Finish")
                    }
                }
            }
        }
    )
}


@Composable
private fun PauseButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Pause, contentDescription = null
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionBottomTabs(
    tabs: ImmutableList<ToolsTab>,
    sheetState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    pagerState: PagerState = rememberPagerState(pageCount = { tabs.size }),
    screenSizeClass: ScreenSizeClass = ScreenSizeDefaults.Phone,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxWidth()) {// full-width container to center tabs column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(
                    min = 0.dp,
                    max = BottomSheetDefaults.SheetMaxWidth
                ) // limit width for large screens
                .background(MaterialTheme.colorScheme.surfaceContainerHigh) // bg for WindowInsets
                .conditional(screenSizeClass.height != WindowHeightSizeClass.Compact) {
                    // take care of navbar insets
                    // (Workaround): but not on landscape mode / compact height because it will add
                    // unnecessary padding on API < 34 (try different navigation bar modes).
                    windowInsetsPadding(WindowInsets.navigationBars)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider()

            ToolsTabRow(
                tabs = tabs,
                activeTabIndex = pagerState.currentPage,
                showIndicator = sheetState.bottomSheetState.currentValue != SheetValue.Hidden,
                onClick = { tabIndex ->
                    scope.launch {
                        val currentPage = pagerState.currentPage

                        // peek sheet when hidden
                        if (sheetState.bottomSheetState.currentValue == SheetValue.Hidden) {
                            pagerState.scrollToPage(tabIndex)    // switch to page (if necessary, no animation)
                            sheetState.bottomSheetState.partialExpand()
                        } else {
                            if (tabIndex == currentPage) {
                                // if on current page, toggle sheet
                                when (sheetState.bottomSheetState.currentValue) {
                                    SheetValue.PartiallyExpanded -> sheetState.bottomSheetState.expand()
                                    SheetValue.Expanded -> sheetState.bottomSheetState.hide()
                                    SheetValue.Hidden -> { /* case should never occur */
                                    }
                                }
                            } else {
                                // if on other page, switch to page (with animation
                                pagerState.animateScrollToPage(tabIndex)
                            }
                        }
                    }
                })
        }
    }
}


@Composable
private fun ToolsTabRow(
    tabs: ImmutableList<ToolsTab>,
    showIndicator: Boolean,
    activeTabIndex: Int,
    onClick: (index: Int) -> Unit,
) {
    TabRow(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        divider = {},
        indicator = { tabPositions ->
            if (activeTabIndex < tabPositions.size) {
                AnimatedVisibility(
                    visible = showIndicator,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
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
    uiState: State<ActiveSessionTimerUiState>,
    sessionState: State<ActiveSessionState>,
    modifier: Modifier = Modifier,
    onResumeTimer: () -> Unit = {},
    screenSizeClass: ScreenSizeClass = ScreenSizeDefaults.Phone,
) {
    Column(
        modifier.animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = MaterialTheme.typography.displayLarge,
            text = uiState.value.timerText,
            fontWeight = FontWeight.Light,
            fontSize = if (screenSizeClass.height == WindowHeightSizeClass.Compact) 60.sp else 75.sp
        )
        when (sessionState.value) {
            ActiveSessionState.PAUSED -> {
                ElevatedButton(
                    onClick = onResumeTimer, colors = ButtonDefaults.elevatedButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(text = uiState.value.subHeadingText)
                }
            }

            else -> {
                Text(
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    text = uiState.value.subHeadingText,
                )
            }

        }
    }
}


@Composable
private fun CurrentPracticingItem(
    uiState: State<ActiveSessionCurrentItemUiState?>,
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass = ScreenSizeDefaults.Phone,
) {
    val item = uiState.value

    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        if (item == null) return@AnimatedVisibility

        val limitedHeight = screenSizeClass.height == WindowHeightSizeClass.Compact

        Surface(
            modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = item.color, shape = MaterialTheme.shapes.large),
            color = item.color.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.large
        ) {
            AnimatedContent(targetState = item.name,
                label = "currentPracticingItem",
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                }) { itemName ->
                Row(
                    modifier = Modifier.height(if (limitedHeight) 42.dp else 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // leading space
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))

                    val textStyle =
                        if (limitedHeight) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.titleLarge

                    Text(
                        modifier = Modifier.weight(1f),
                        text = itemName,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = item.durationText,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface,
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
    uiState: State<ActiveSessionCompletedSectionsUiState?>,
    additionalBottomContentPadding: Dp = 0.dp,
    onSectionDeleted: (CompletedSectionUiState) -> Unit = {},
    nestedScrollConnection: NestedScrollConnection = rememberNestedScrollInteropConnection(),
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listUiState = uiState.value ?: return

    // This column must not have padding to make swipe-to-dismiss work edge2edge
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large),
            textAlign = TextAlign.Start,
            text = "Already practiced",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fadingEdge(listState)
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .padding(
                    horizontal = MaterialTheme.spacing.large    // main padding
                ),
            contentPadding = PaddingValues(bottom = additionalBottomContentPadding)
        ) {
            items(
                items = listUiState.items,
                key = { item -> item.id },
            ) { item ->
                SectionListElement(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    snackbarHostState = snackbarHostState,
                    onSectionDeleted = onSectionDeleted,
                )
            }
        }
    }

    // scroll to top when new item is added
    var sectionLen by remember { mutableIntStateOf(listUiState.items.size) }
    LaunchedEffect(key1 = listUiState.items) {
        if (listUiState.items.size > sectionLen && listState.canScrollBackward) {
            listState.animateScrollToItem(0)
        }
        sectionLen = listUiState.items.size
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    item: CompletedSectionUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSectionDeleted: (CompletedSectionUiState) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            deleted = targetValue == SwipeToDismissBoxValue.EndToStart
            true// don't set to deleted or item will not be dismissable again after restore
        },
        positionalThreshold = with(LocalDensity.current) {
            { 100.dp.toPx() } // TODO remove hardcode?
        }
    )
    SwipeToDeleteContainer(
        state = dismissState,
        deleted = deleted,
        onDeleted = {
            scope.launch {
                // TODO handle deletion when user leaves screen before timeout
                val result = snackbarHostState.showSnackbar(
                    message = "Section deleted",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        deleted = false
                        dismissState.reset()
                    }

                    SnackbarResult.Dismissed -> {
                        onSectionDeleted(item)
                    }
                }
            }
        }
    ) {
        Surface(
            // Surface for setting shape of item container
            modifier = modifier.height(50.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(item.color.copy(alpha = 0.6f)),
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
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            }
        }
    }
}

@Composable
private fun AddSectionFAB(
    sessionState: State<ActiveSessionState>,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier.padding(MaterialTheme.spacing.large),
        enter = slideInVertically(initialOffsetY = { it * 2 }),
        exit = slideOutVertically(targetOffsetY = { it * 2 }),
    ) {
        // FAB for new Item
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add, contentDescription = "New Library Item"
                )
            },
            text = {
                Text(
                    if (sessionState.value == ActiveSessionState.NOT_STARTED) {
                        "Start practicing"
                    } else {
                        "Next Item"
                    }
                )
            },
            expanded = true,
        )
    }
}

@Composable
private fun NewItemSelector(
    uiState: State<NewItemSelectorUiState>,
    modifier: Modifier = Modifier,
    onItemSelected: (LibraryItem) -> Unit,
    onNewItem: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    var selectedFolder: UUID? by remember { mutableStateOf(uiState.value.runningItem?.libraryFolderId) }
    var createMenuShown by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {

        // Header + Close Button
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Row(
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

            IconButton(
                onClick = { createMenuShown = true },
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
            ) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)

                DropdownMenu(
                    expanded = createMenuShown,
                    onDismissRequest = { createMenuShown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {

                    DropdownMenuItem(
                        onClick = onNewItem,
                        text = { Text(text = "Create Item") }
                    )
                    DropdownMenuItem(
                        onClick = onNewFolder,
                        text = { Text(text = "Create Folder") }
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Folders
        val folders =
            remember { uiState.value.foldersWithItems.map { it.folder }.toImmutableList() }
        if (folders.isNotEmpty()) {
            LibraryFoldersRow(
                folders = folders,
                highlightedFolderId = selectedFolder,
                showBadge = uiState.value.runningItem != null,
                folderWithBadge = uiState.value.runningItem?.libraryFolderId,
                onFolderSelected = remember {
                    { folderId ->
                        selectedFolder = folderId
                    }
                })
        }

        // use own divider to avoid padding of default one from TabRow
        // and also to show it when folders are not shown
        HorizontalDivider()

        val items = remember(selectedFolder) {
            // all items of the selected folder or root items if not found in folders
            uiState.value.foldersWithItems.firstOrNull {
                it.folder.id == selectedFolder
            }?.items?.toImmutableList() ?: uiState.value.rootItems.toImmutableList()
        }

        // Items
        LibraryItemList(
            items = items,
            // TODO update last practiced Dates for items during session
            lastPracticedDates = uiState.value.lastPracticedDates.toImmutableMap(),
            onItemClick = { libraryItem ->
                onItemSelected(libraryItem)
                onClose()
            }, activeItemId = uiState.value.runningItem?.id
        )
    }
}

@Composable
private fun LibraryFoldersRow(
    modifier: Modifier = Modifier,
    folders: ImmutableList<LibraryFolder>,
    showBadge: Boolean = true,
    highlightedFolderId: UUID?,
    folderWithBadge: UUID?,
    onFolderSelected: (UUID?) -> Unit,
) {
    // translate highlightedFolderId to selectedTabIndex
    // since the first tab is the "no folder" tab, add +1 (note: indexOfFirst returns -1 if not found)
    val selectedTabIndex = remember(highlightedFolderId) {
        folders.indexOfFirst { it.id == highlightedFolderId } + 1
    }

    ScrollableTabRow(
        modifier = modifier.fillMaxWidth(), selectedTabIndex = selectedTabIndex,
        divider = { }
    ) {

        LibraryFolderElement(
            folder = null,
            onClick = { onFolderSelected(null) },
            isSelected = highlightedFolderId == null,
            showBadge = showBadge && folderWithBadge == null
        )

        folders.forEach { folder ->
            LibraryFolderElement(
                folder = folder,
                onClick = { onFolderSelected(folder.id) },
                isSelected = folder.id == highlightedFolderId,
                showBadge = showBadge && folder.id == folderWithBadge
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

    Tab(
        modifier = Modifier.size(70.dp),
        selected = isSelected,
        onClick = { onClick(folder) },
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
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.small)
                .basicMarquee(),
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
    lastPracticedDates: ImmutableMap<UUID, ZonedDateTime?>,
    onItemClick: (LibraryItem) -> Unit,
    activeItemId: UUID?,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = WindowInsets(
            top = MaterialTheme.spacing.small,
        ).add(WindowInsets.navigationBars).asPaddingValues()    // don't get covered by navbars
    ) {
        items(items) {
            LibraryUiItem(
                item = it,
                lastPracticedDate = lastPracticedDates.filterKeys { key -> key == it.id }.values.firstOrNull(),
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
    onRatingChanged: (Int) -> Unit = { _ -> },
    onCommentChanged: (String) -> Unit = { _ -> },
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column {
                DialogHeader(title = "Finish session")

                Column(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {

                    Text(text = "Rate your session: ")
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
                            onRatingChanged = onRatingChanged,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.large))
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

        ActiveSessionScreen(
            sizeClass = ScreenSizeDefaults.Phone,
            uiState = remember {
                mutableStateOf(
                    ActiveSessionUiState(
                        sessionState = MutableStateFlow(ActiveSessionState.RUNNING),
                        mainContentUiState = MutableStateFlow(
                            ActiveSessionContentUiState(
                                timerUiState = MutableStateFlow(
                                    ActiveSessionTimerUiState(
                                        timerText = getDurationString(
                                            (42 * 60 + 24).seconds, DurationFormat.MS_DIGITAL
                                        ).toString(),
                                        subHeadingText = "Practice Time",
                                    )
                                ),
                                currentItemUiState = MutableStateFlow(dummyRunningItem),
                                pastSectionsUiState = MutableStateFlow(
                                    ActiveSessionCompletedSectionsUiState(
                                        items = dummySections.toList()
                                    )
                                ),
                            )
                        ),
                        toolsUiState = MutableStateFlow(
                            ActiveSessionToolsUiState(
                                expanded = true,
                                activeTab = ActiveSessionTab.METRONOME,
                            )
                        ),
                        newItemSelectorUiState = MutableStateFlow(
                            NewItemSelectorUiState(
                                rootItems = dummyLibraryItems.toList(),
                                foldersWithItems = dummyFolders.map {
                                    LibraryFolderWithItems(
                                        it,
                                        dummyLibraryItems.toList()
                                    )
                                }.toList(),
                                runningItem = dummyLibraryItems.first().copy(
                                    libraryFolderId = UUIDConverter.fromInt(1)
                                )
                            )
                        ),
                    )
                )
            },
            tabs = listOf(
                ToolsTab(
                    type = ActiveSessionTab.METRONOME,
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
        CurrentPracticingItem(
            uiState = remember {
                mutableStateOf(dummyRunningItem)
            })
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
        LibraryFoldersRow(folders = dummyFolders.toImmutableList(),
            highlightedFolderId = dummyFolders.first().id,
            folderWithBadge = dummyFolders.toList()[2].id,
            onFolderSelected = {})
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
                uiState = remember {
                    mutableStateOf(
                        NewItemSelectorUiState(
                            foldersWithItems = dummyFolders.map {
                                LibraryFolderWithItems(
                                    it,
                                    dummyLibraryItems.toList()
                                )
                            }.toList(),
                            rootItems = dummyLibraryItems.toList(),
                            runningItem = dummyLibraryItems.first().copy(
                                libraryFolderId = UUIDConverter.fromInt(1)
                            )
                        )
                    )
                },
                onItemSelected = { })
        }
    }
}

@Preview(name = "No Folders", group = "Element 4", showSystemUi = true)
@Composable
private fun PreviewNewItemSelectorNoFolders() {
    MusikusThemedPreview {
        Column {
            NewItemSelector(
                uiState = remember {
                    mutableStateOf(
                        NewItemSelectorUiState(
                            foldersWithItems = emptyList(),
                            runningItem = dummyLibraryItems.first()
                        )
                    )
                },
                onItemSelected = { })
        }
    }
}

@Preview(name = "No Folders", group = "Element 4", showSystemUi = true)
@Composable
private fun PreviewNewItemSelectorOneFolders() {
    MusikusThemedPreview {
        Column {
            NewItemSelector(
                uiState = remember {
                    mutableStateOf(
                        NewItemSelectorUiState(
                            foldersWithItems = dummyFolders.take(1).map {
                                LibraryFolderWithItems(it, dummyLibraryItems.toList())
                            }.toList(),
                            runningItem = dummyLibraryItems.first()
                        )
                    )
                },
                onItemSelected = { })
        }
    }
}

@MusikusPreviewElement5
@Composable
private fun PreviewLibraryItem(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        LibraryUiItem(item = dummyLibraryItems.first(),
            lastPracticedDate = ZonedDateTime.now(),
            selected = false,
            onShortClick = { /*TODO*/ },
            onLongClick = { /*TODO*/ })
    }
}

@MusikusPreviewElement6
@Composable
private fun PreviewEndSessionDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        EndSessionDialog(
            rating = 3,
            comment = "This was a great session",
        )
    }
}


/** -------------------------------- Preview Parameter Providers -------------------------------- */


private val dummyRunningItem = ActiveSessionCurrentItemUiState(
    color = libraryItemColors[Random.nextInt(libraryItemColors.size)],
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