/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Michael Prommersberger, Matthias Emde
 */
package app.musikus.activesession.presentation

import android.app.Activity
import android.widget.Toast
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.musikus.R
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.components.DeleteConfirmationBottomSheet
import app.musikus.core.presentation.components.ExceptionHandler
import app.musikus.core.presentation.components.MusikusShowcaseDialog
import app.musikus.core.presentation.components.conditional
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewWholeScreen
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.dimensions
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.metronome.presentation.MetronomeUi
import app.musikus.recorder.presentation.RecorderUi
import com.joco.showcase.sequence.SequenceShowcase
import com.joco.showcase.sequence.SequenceShowcaseScope
import com.joco.showcase.sequence.SequenceShowcaseState
import com.joco.showcase.sequence.rememberSequenceShowcaseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Actions that can be triggered by the Notification
 */
@Keep
@Serializable
enum class ActiveSessionActions {
    PAUSE, FINISH, METRONOME, RECORDER
}

/**
 * The order in which UI items of this screen are introduced by app intro dialogs.
 *
 * Note: when a UI item is not (yet) visible, the dialog will not be shown and it will not be possible to continue
 * until the item is visible.
 */
private enum class IntroOrder(val index: Int) {
    FAB_START(0),         // start with the FAB to start a new section
    CURRENT_SECTION(1),   // introduce the current section item
    PRACTICE_TIMER(2),    // introduce the practice timer
    MINIMIZE_BUTTON(3),   // introduce the push to background button
    PAUSE_BUTTON(4),      // introduce the action buttons: pause,...
    DISCARD_BUTTON(5),    // ...discard, and
    FAB_NEXT_SECTION(6),  // introduce the FAB to add a new section
    PAST_SECTIONS(7),     // introduce the list of past sections
    FINSIH_BUTTON(8),     // ...finish button
    RESUME_BUTTON(9),     // introduce the resume button in the practice timer when the user pauses for the first time
}

data class ToolsTab(
    val title: String,
    val icon: UiIcon,
    val type: ActiveSessionTab,
    val content: @Composable () -> Unit,
)

data class ScreenSizeClass(
    val width: WindowWidthSizeClass,
    val height: WindowHeightSizeClass,
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun ActiveSession(
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    mainEventHandler: MainUiEventHandler,
    deepLinkAction: ActiveSessionActions? = null,
    navigateUp: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent
    val scope = rememberCoroutineScope()
    val windowsSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            skipHiddenState = false,
            // initialValue = SheetValue.Hidden does not work here unfortunately
        )
    )
    // TODO move to somewhere final
    val tabs = persistentListOf(
        ToolsTab(
            type = ActiveSessionTab.METRONOME,
            title = stringResource(id = R.string.active_session_toolbar_metronome),
            icon = UiIcon.IconResource(R.drawable.ic_metronome),
            content = { MetronomeUi() }
        ),
        ToolsTab(
            type = ActiveSessionTab.RECORDER,
            title = stringResource(id = R.string.active_session_toolbar_recorder),
            icon = UiIcon.DynamicIcon(Icons.Default.Mic),
            content = { RecorderUi(showSnackbar = { mainEventHandler(it) }) }
        )
    ).toImmutableList()
    // state for Tabs
    val bottomSheetPagerState = rememberPagerState(pageCount = { tabs.size })

    /**
     * Observe events sent from the viewmodel
     */
    ObserveAsEvents(viewModel.eventChannel) { event ->
        when (event) {
            is ActiveSessionEvent.HideTools -> {
                scope.launch {
                    bottomSheetScaffoldState.bottomSheetState.hide()
                }
            }
        }
    }

    /**
     * Keep screen on while session is running
     */
    val sessionState by uiState.value.sessionState.collectAsState()

    val view = LocalView.current
    LaunchedEffect(sessionState) {
        view.keepScreenOn = sessionState == ActiveSessionState.RUNNING
    }

    /**
     * Exception handling
     */
    val context = LocalContext.current
    ExceptionHandler<ActiveSessionException>(
        viewModel.exceptionChannel,
        exceptionHandler = { exception ->
            Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
        },
        onUnhandledException = { throw (it) }
    )

    ActiveSessionScreen(
        uiState = uiState,
        eventHandler = eventHandler,
        navigateUp = navigateUp,
        tabs = tabs,
        bottomSheetScaffoldState = bottomSheetScaffoldState,
        bottomSheetPagerState = bottomSheetPagerState,
        showSnackbar = { mainEventHandler(it) },
        sizeClass = ScreenSizeClass(
            windowsSizeClass.widthSizeClass,
            windowsSizeClass.heightSizeClass
        )
    )

    /** Handle deep link Arguments */
    LaunchedEffect(deepLinkAction) {
        when (deepLinkAction) {
            ActiveSessionActions.METRONOME -> {
                // switch to metronome tab
                scope.launch {
                    bottomSheetPagerState.animateScrollToPage(
                        tabs.indexOfFirst { it.type == ActiveSessionTab.METRONOME }
                    )
                }
                // expand bottom sheet
                scope.launch {
                    bottomSheetScaffoldState.bottomSheetState.expand()
                }
            }

            ActiveSessionActions.RECORDER -> {
                // switch to metronome tab
                scope.launch {
                    bottomSheetPagerState.animateScrollToPage(
                        tabs.indexOfFirst { it.type == ActiveSessionTab.RECORDER }
                    )
                }
                // expand bottom sheet
                scope.launch {
                    bottomSheetScaffoldState.bottomSheetState.expand()
                }
            }

            ActiveSessionActions.FINISH -> {
                eventHandler(ActiveSessionUiEvent.ToggleFinishDialog)
            }

            else -> {
                // do nothing
            }
        }
    }
}

@Composable
private fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionScreen(
    uiState: State<ActiveSessionUiState>,
    tabs: ImmutableList<ToolsTab>,
    eventHandler: ActiveSessionUiEventHandler,
    navigateUp: () -> Unit,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    bottomSheetPagerState: PagerState,
    sizeClass: ScreenSizeClass,
    showSnackbar: (MainUiEvent.ShowSnackbar) -> Unit,
) {
    val sequenceShowcaseState = rememberSequenceShowcaseState()
    SequenceShowcase(state = sequenceShowcaseState) {

        // Custom Scaffold for our elements which adapts to available window sizes
        ActiveSessionAdaptiveScaffold(
            screenSizeClass = sizeClass,
            bottomSheetScaffoldState = bottomSheetScaffoldState,
            topBar = {
                ActiveSessionTopBar(
                    sessionState = uiState.value.sessionState.collectAsState(),
                    isFinishedButtonEnabled = uiState.value.isFinishButtonEnabled.collectAsState(),
                    onDiscard = remember { { eventHandler(ActiveSessionUiEvent.ToggleDiscardDialog) } },
                    onNavigateUp = remember { { navigateUp() } },
                    onTogglePause = remember { { eventHandler(ActiveSessionUiEvent.TogglePauseState) } },
                    onSave = remember { { eventHandler(ActiveSessionUiEvent.ToggleFinishDialog) } },
                    eventHandler = eventHandler,
                    sequenceShowcaseState = sequenceShowcaseState,
                )
            },
            bottomBar = {
                ActiveSessionBottomTabs(
                    tabs = tabs,
                    sheetState = bottomSheetScaffoldState,
                    pagerState = bottomSheetPagerState,
                    screenSizeClass = sizeClass,
                )
            },
            mainContent = { padding ->
                ActiveSessionMainContent(
                    contentPadding = padding,
                    uiState = uiState.value.mainContentUiState.collectAsState(),
                    sessionState = uiState.value.sessionState.collectAsState(),
                    showSnackbar = showSnackbar,
                    eventHandler = eventHandler,
                    screenSizeClass = sizeClass,
                    sequenceShowcaseState = sequenceShowcaseState,
                )
            },
            toolsContent = {
                ActiveSessionToolsLayout(
                    tabs = tabs,
                    pagerState = bottomSheetPagerState
                )
            }
        )
    }

    /**
     * --------------------- Dialogs ---------------------
     */

    /** New Item Selector */
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val newItemSelectorState = uiState.value.newItemSelectorUiState.collectAsState()

    if (newItemSelectorState.value != null) {
        NewItemSelectorBottomSheet(
            uiState = newItemSelectorState,
            sheetState = sheetState,
            eventHandler = remember { { eventHandler(ActiveSessionUiEvent.NewItemSelectorEvent(it)) } },
            onDismissed = remember { { eventHandler(ActiveSessionUiEvent.ToggleNewItemSelector) } },
        )
    }

    /** End Session Dialog */
    val dialogUiState = uiState.value.dialogUiState.collectAsState()
    val endDialogUiState = dialogUiState.value.endDialogUiState
    if (endDialogUiState != null) {
        val dialogEvent = ActiveSessionUiEvent::EndDialogUiEvent

        EndSessionDialog(
            rating = endDialogUiState.rating,
            comment = endDialogUiState.comment,
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
                eventHandler(dialogEvent(ActiveSessionEndDialogUiEvent.Confirmed))
                navigateUp()
            }
        )
    }

    /** Discard Session Dialog */
    if (dialogUiState.value.discardDialogVisible) {
        DeleteConfirmationBottomSheet(
            explanation = UiText.StringResource(R.string.active_session_discard_session_dialog_explanation),
            confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
            confirmationText = UiText.StringResource(R.string.active_session_discard_session_dialog_confirm),
            onDismiss = { eventHandler(ActiveSessionUiEvent.ToggleDiscardDialog) },
            onConfirm = {
                eventHandler(ActiveSessionUiEvent.DiscardSessionDialogConfirmed)
                navigateUp()
            }
        )
    }

    // Side effect to start the sequence showcase dialog every time the screen is shown
    SideEffect {
        sequenceShowcaseState.start(dialogUiState.value.appIntroDialogIndex)
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
    bottomSheetScaffoldState: BottomSheetScaffoldState,
) {
    if (screenSizeClass.height == WindowHeightSizeClass.Compact) {
        /** Landscape / small height. Use two columns with main content left, bottom sheet right. */

        Scaffold(
            // Scaffold needed for topBar
            modifier = modifier,
            topBar = topBar,
        ) {
            Row(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                Column( // left column
                    Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                ) {
                    mainContent(remember { mutableStateOf(PaddingValues(0.dp)) })
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                Column( // right column
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Scaffold( // Scaffold needed for bottomBar
                        bottomBar = bottomBar,
                        content = { paddingValues ->
                            Surface( // don't overlap with bottomBar
                                Modifier.padding(bottom = paddingValues.calculateBottomPadding())
                            ) {
                                ToolsBottomSheetScaffold(
                                    bottomSheetScaffoldState = bottomSheetScaffoldState,
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
            content = { paddingValues ->
                Surface(Modifier.padding(paddingValues)) { // don't overlap with bottomBar
                    ToolsBottomSheetScaffold(
                        bottomSheetScaffoldState = bottomSheetScaffoldState,
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
    bottomSheetScaffoldState: BottomSheetScaffoldState,
) {
    BottomSheetScaffold(
        sheetContent = {
            Column(modifier = Modifier.animateContentSize()) {
                sheetContent()
            }
        },
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetTonalElevation = 0.dp, // deprecated anyways
        sheetShadowElevation = 0.dp, // deprecated anyways
        sheetPeekHeight = MaterialTheme.dimensions.toolsSheetPeekHeight,
        scaffoldState = bottomSheetScaffoldState,
        sheetDragHandle = { SheetDragHandle() },
        content = { sheetPadding ->
            // sheetPadding does not work when Hidden, so manually force 0dp padding
            val bottomPadding = animateDpAsState(
                if (bottomSheetScaffoldState.bottomSheetState.currentValue != SheetValue.Hidden) {
                    sheetPadding.calculateBottomPadding()
                } else {
                    0.dp
                },
                label = "animatedBottomPadding"
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
private fun SequenceShowcaseScope.ActiveSessionMainContent(
    modifier: Modifier = Modifier,
    screenSizeClass: ScreenSizeClass,
    contentPadding: State<PaddingValues>,
    uiState: State<ActiveSessionContentUiState>,
    sessionState: State<ActiveSessionState>,
    showSnackbar: (MainUiEvent.ShowSnackbar) -> Unit,
    eventHandler: ActiveSessionUiEventHandler,
    sequenceShowcaseState: SequenceShowcaseState,
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

            Column( // Animated container
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
                    onResumeTimer = remember {
                        {
                            sequenceShowcaseState.dismiss() // dismiss showcase dialog when resuming timer
                            eventHandler(ActiveSessionUiEvent.TogglePauseState)
                        }
                    },
                    modifier = Modifier.sequenceShowcaseTarget(
                        index = IntroOrder.PRACTICE_TIMER.index,
                        content = {
                            MusikusShowcaseDialog(
                                targetRect = it,
                                text = UiText.StringResource(resId = R.string.active_session_app_intro_practice_timer),
                                onClick = {
                                    introDialogConfirmed(
                                        index = IntroOrder.PRACTICE_TIMER.index,
                                        eventHandler = eventHandler,
                                        state = sequenceShowcaseState
                                    )
                                }
                            )
                        }
                    ),
                    modifierPausedButton = Modifier.sequenceShowcaseTarget(
                        index = IntroOrder.RESUME_BUTTON.index,
                        content = {
                            MusikusShowcaseDialog(
                                targetRect = it,
                                text = UiText.StringResource(resId = R.string.active_session_app_intro_resume_button),
                                onClick = {
                                    introDialogConfirmed(
                                        index = IntroOrder.RESUME_BUTTON.index,
                                        eventHandler = eventHandler,
                                        state = sequenceShowcaseState
                                    )
                                }
                            )
                        }
                    ),
                )

                if (limitedHeight) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                } else {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                }

                // Running Item
                CurrentPracticingItem(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.large)
                        .sequenceShowcaseTarget(
                            index = IntroOrder.CURRENT_SECTION.index,
                            content = {
                                MusikusShowcaseDialog(
                                    targetRect = it,
                                    text = UiText.StringResource(resId = R.string.active_session_app_intro_current_section),
                                    onClick = {
                                        introDialogConfirmed(
                                            index = IntroOrder.CURRENT_SECTION.index,
                                            eventHandler = eventHandler,
                                            state = sequenceShowcaseState
                                        )
                                    }
                                )
                            }
                        ),
                    screenSizeClass = screenSizeClass,
                    uiState = uiState.value.currentItemUiState.collectAsState(),
                )

                if (limitedHeight) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                } else {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
                }

                // Past Items
                val pastItemsState = uiState.value.pastSectionsUiState.collectAsState()
                if (pastItemsState.value != null) {
                    SectionsList(
                        modifier = Modifier.sequenceShowcaseTarget(
                            index = IntroOrder.PAST_SECTIONS.index,
                            content = {
                                MusikusShowcaseDialog(
                                    targetRect = it,
                                    text = UiText.StringResource(resId = R.string.active_session_app_intro_past_sections),
                                    onClick = {
                                        introDialogConfirmed(
                                            index = IntroOrder.PAST_SECTIONS.index,
                                            eventHandler = eventHandler,
                                            state = sequenceShowcaseState
                                        )
                                    }
                                )
                            }
                        ),
                        uiState = pastItemsState,
                        nestedScrollConnection = nestedScrollConnection, // for hiding the FAB
                        listState = sectionsListState,
                        showSnackbar = showSnackbar,
                        onSectionDeleted = remember {
                            { section ->
                                introDialogConfirmed(
                                    index = IntroOrder.PAST_SECTIONS.index,
                                    eventHandler = eventHandler,
                                    state = sequenceShowcaseState
                                )
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
            isVisible = addSectionFABVisible || !limitedHeight, // only hide FAB in landscape layout
            sessionState = sessionState,
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = remember { { eventHandler(ActiveSessionUiEvent.ToggleNewItemSelector) } },
            sequenceShowcaseState = sequenceShowcaseState,
            appIntroIndex = if (sessionState.value == ActiveSessionState.NOT_STARTED) {
                IntroOrder.FAB_START.index
            } else {
                IntroOrder.FAB_NEXT_SECTION.index
            },
            eventHandler = eventHandler,
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionToolsLayout(
    modifier: Modifier = Modifier,
    tabs: ImmutableList<ToolsTab>,
    pagerState: PagerState,
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
private fun SequenceShowcaseScope.ActiveSessionTopBar(
    sessionState: State<ActiveSessionState>,
    isFinishedButtonEnabled: State<Boolean>,
    onDiscard: () -> Unit,
    onNavigateUp: () -> Unit,
    onTogglePause: () -> Unit,
    onSave: () -> Unit,
    eventHandler: ActiveSessionUiEventHandler,
    sequenceShowcaseState: SequenceShowcaseState
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(
                modifier = Modifier.sequenceShowcaseTarget(
                    index = IntroOrder.MINIMIZE_BUTTON.index,
                    content = {
                        MusikusShowcaseDialog(
                            targetRect = it,
                            text = UiText.StringResource(resId = R.string.active_session_app_intro_minimize_button),
                            onClick = {
                                introDialogConfirmed(
                                    index = IntroOrder.MINIMIZE_BUTTON.index,
                                    eventHandler = eventHandler,
                                    state = sequenceShowcaseState
                                )
                            }
                        )
                    }
                ),
                onClick = {
                    introDialogConfirmed(
                        index = IntroOrder.MINIMIZE_BUTTON.index,
                        eventHandler = eventHandler,
                        state = sequenceShowcaseState
                    )
                    onNavigateUp()
                },
                content = {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                })
        },
        actions = {
            AnimatedVisibility(
                visible = sessionState.value == ActiveSessionState.RUNNING ||
                        sessionState.value == ActiveSessionState.PAUSED,
                enter = slideInVertically(),
            ) {
                Row {
//                    AnimatedVisibility(
//                        visible = sessionState.value == ActiveSessionState.RUNNING,
//                        enter = fadeIn(),
//                        exit = fadeOut(),
//                    ) {
                    PauseButton(
                        modifier = Modifier.sequenceShowcaseTarget(
                            index = IntroOrder.PAUSE_BUTTON.index,
                            content = {
                                MusikusShowcaseDialog(
                                    targetRect = it,
                                    text = UiText.StringResource(resId = R.string.active_session_app_intro_pause_button),
                                    onClick = {
                                        introDialogConfirmed(
                                            index = IntroOrder.PAUSE_BUTTON.index,
                                            eventHandler = eventHandler,
                                            state = sequenceShowcaseState
                                        )
                                    }
                                )
                            }
                        ),
                        onClick = {
                            introDialogConfirmed(
                                index = IntroOrder.PAUSE_BUTTON.index,
                                eventHandler = eventHandler,
                                state = sequenceShowcaseState
                            )
                            onTogglePause()
                        }
                    )
//                    }

                    IconButton(
                        modifier = Modifier.sequenceShowcaseTarget(
                            index = IntroOrder.DISCARD_BUTTON.index,
                            content = {
                                MusikusShowcaseDialog(
                                    targetRect = it,
                                    text = UiText.StringResource(resId = R.string.active_session_app_intro_discard_button),
                                    onClick = {
                                        introDialogConfirmed(
                                            index = IntroOrder.DISCARD_BUTTON.index,
                                            eventHandler = eventHandler,
                                            state = sequenceShowcaseState
                                        )
                                    }
                                )
                            }
                        ),
                        onClick = {
                            introDialogConfirmed(
                                index = IntroOrder.DISCARD_BUTTON.index,
                                eventHandler = eventHandler,
                                state = sequenceShowcaseState
                            )
                            onDiscard()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.active_session_top_bar_delete)
                        )
                    }

                    TextButton(
                        modifier = Modifier.sequenceShowcaseTarget(
                            index = IntroOrder.FINSIH_BUTTON.index,
                            content = {
                                MusikusShowcaseDialog(
                                    targetRect = it,
                                    text = UiText.StringResource(resId = R.string.active_session_app_intro_finish_button),
                                    onClick = {
                                        introDialogConfirmed(
                                            index = IntroOrder.FINSIH_BUTTON.index,
                                            eventHandler = eventHandler,
                                            state = sequenceShowcaseState
                                        )
                                    }
                                )
                            }
                        ),
                        onClick = {
                            introDialogConfirmed(
                                index = IntroOrder.FINSIH_BUTTON.index,
                                eventHandler = eventHandler,
                                state = sequenceShowcaseState
                            )
                            onSave()
                        },
                        enabled = isFinishedButtonEnabled.value
                    ) {
                        Text(text = stringResource(id = R.string.active_session_top_bar_save))
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionBottomTabs(
    tabs: ImmutableList<ToolsTab>,
    sheetState: BottomSheetScaffoldState,
    pagerState: PagerState,
    screenSizeClass: ScreenSizeClass,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxWidth()) { // full-width container to center tabs column
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
                            pagerState.scrollToPage(tabIndex) // switch to page (if necessary, no animation)
                            sheetState.bottomSheetState.partialExpand()
                        } else {
                            if (tabIndex == currentPage) {
                                // if on current page, toggle sheet
                                when (sheetState.bottomSheetState.currentValue) {
                                    SheetValue.PartiallyExpanded -> sheetState.bottomSheetState.expand()
                                    SheetValue.Expanded -> sheetState.bottomSheetState.hide()
                                    SheetValue.Hidden -> {
                                        /* case should never occur */
                                    }
                                }
                            } else {
                                // if on other page, switch to page (with animation
                                pagerState.scrollToPage(tabIndex)
                            }
                        }
                    }
                }
            )
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
                            .offset(y = (-69).dp),
                        width = 100.dp,
                        shape = RoundedCornerShape(
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
                Tab(
                    selected = activeTabIndex == index,
                    text = { Text(tab.title) },
                    icon = { Icon(imageVector = tab.icon.asIcon(), contentDescription = null) },
                    onClick = { onClick(index) }
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Surface(
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.small)
            .size(
                width = 25.dp,
                height = 3.dp
            ),
        shape = RoundedCornerShape(50),
        content = { }
    )
}

private fun introDialogConfirmed(
    index: Int,
    state: SequenceShowcaseState,
    eventHandler: ActiveSessionUiEventHandler,
) {
    if (eventHandler(ActiveSessionUiEvent.AppIntroDialogConfirmed(index))) {
        state.dismiss()  // proceed to next item next time
        return
    }
    state.dismiss()  // just dismiss if false (invalid call)
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
    val mainContent = ActiveSessionContentUiState(
        timerUiState = MutableStateFlow(
            ActiveSessionTimerUiState(
                timerText = getDurationString(
                    (42 * 60 + 24).seconds,
                    DurationFormat.MS_DIGITAL
                ).toString(),
                subHeadingText = UiText.StringResource(R.string.active_session_timer_subheading),
            )
        ),
        currentItemUiState = MutableStateFlow(dummyRunningItem),
        pastSectionsUiState = MutableStateFlow(
            ActiveSessionCompletedSectionsUiState(
                items = dummySections.toList()
            )
        ),
    )

    val dialogs = ActiveSessionDialogsUiState(
        endDialogUiState = null,
        discardDialogVisible = false,
        appIntroDialogIndex = 100, // not shown in preview
    )

    MusikusThemedPreview(theme) {
        ActiveSessionScreen(
            sizeClass = ScreenSizeDefaults.Phone,
            uiState = remember {
                mutableStateOf(
                    ActiveSessionUiState(
                        sessionState = MutableStateFlow(ActiveSessionState.RUNNING),
                        mainContentUiState = MutableStateFlow(mainContent),
                        newItemSelectorUiState = MutableStateFlow(null),
                        dialogUiState = MutableStateFlow(dialogs),
                        isFinishButtonEnabled = MutableStateFlow(true)
                    )
                )
            },
            tabs = listOf(
                ToolsTab(
                    type = ActiveSessionTab.METRONOME,
                    title = stringResource(id = R.string.active_session_toolbar_metronome),
                    icon = UiIcon.IconResource(R.drawable.ic_metronome),
                    content = { }
                ),
                ToolsTab(
                    type = ActiveSessionTab.RECORDER,
                    title = stringResource(id = R.string.active_session_toolbar_recorder),
                    icon = UiIcon.DynamicIcon(Icons.Default.Mic),
                    content = { }
                )
            ).toImmutableList(),
            eventHandler = { true },
            navigateUp = {},
            bottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
            bottomSheetPagerState = rememberPagerState(pageCount = { 2 }),
            showSnackbar = {}
        )
    }
}


/** -------------------------------- Preview Parameter Providers -------------------------------- */

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

internal val dummyRunningItem = ActiveSessionCurrentItemUiState(
    color = libraryItemColors[Random.nextInt(libraryItemColors.size)],
    name = LoremIpsum(Random.nextInt(4, 10)).values.first(),
    durationText = "32:19",
)
