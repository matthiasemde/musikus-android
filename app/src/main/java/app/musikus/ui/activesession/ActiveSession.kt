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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.daos.LibraryFolder
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
import kotlin.time.Duration


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


    BottomSheetScaffold(
        sheetContent = {
            ToolsLayout(tabs = tabs)
        },
        sheetDragHandle = {},
        sheetPeekHeight = 200.dp
    ) { paddingValues ->

        Surface(
            modifier = Modifier.padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                HeaderBar(uiState, eventHandler, navigateUp)

                /** ################################## MAIN UI CONTENT ################################## */

                /** ################################## MAIN UI CONTENT ################################## */

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PracticeTimer(
                        modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                        uiState = uiState
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
                    CurrentPracticingItem(item = uiState.runningSection)
                    SectionsList(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        uiState = uiState,
                        onSectionDeleted = { eventHandler(ActiveSessionUiEvent.DeleteSection(it.id)) },
                        additionalBottomContentPadding = 28.dp    // padding for the FAB (half of FAB height)
                    )

                    ExtendedFloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-28).dp),  // offset to center the FAB (FAB height = 56.dp)
                        onClick = { showLibrary = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "New Library Item"
                            )
                        },
                        text = { Text("New Library Item") },
                        expanded = true
                    )

                }

            }
        }

    }


    /**
     * --------------------- Dialogs ---------------------
     */

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    if (showLibrary) {
        ModalBottomSheet(
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

    if (uiState.isPaused) {
        PauseDialog(
            uiState.ongoingPauseDuration,
            eventHandler
        )
    }

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

@Composable
private fun ToolsLayout(
    tabs: List<ToolsTab>
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column {
        ToolsTabRow(
            tabs = tabs,
            activeTabIndex = pagerState.currentPage,
            onClick = { tabIndex ->
                scope.launch {
                    pagerState.animateScrollToPage(tabIndex)
                }
            }
        )
        HorizontalPager(state = pagerState) { tabIndex ->
            ToolsCardLayout { tabs[tabIndex].content() }
        }
    }
}

@Composable
private fun ToolsTabRow(
    tabs: List<ToolsTab>,
    activeTabIndex: Int,
    onClick: (index: Int) -> Unit
) {
    TabRow(selectedTabIndex = activeTabIndex) {
        tabs.forEachIndexed { index, tab ->
            LeadingIconTab(
                selected = activeTabIndex == index,
                text = { Text(tab.title) },
                onClick = { onClick(index) },
                icon = tab.icon
            )
        }
    }
}

@Composable
private fun ToolsCardLayout(
    content: @Composable () -> Unit
) {
    OutlinedCard(
        Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.large)
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
private fun HeaderBar(
    uiState: ActiveSessionUiState,
    eventHandler: ActiveSessionUiEventHandler,
    navigateUp: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
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
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    OutlinedButton(
                        onClick = { eventHandler(ActiveSessionUiEvent.TogglePause) }
                    ) {
                        Text(text = "Pause")
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = uiState.runningSection != null,
            enter = slideInVertically(),
        ) {
            TextButton(
                onClick = {
                    eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
                }
            ) {
                Text(text = "Save Session")
            }
        }
    }
}


@Composable
private fun PracticeTimer(
    modifier: Modifier = Modifier,
    uiState: ActiveSessionUiState,
) {
    Text(
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge,
        text = getDurationString(
            uiState.totalSessionDuration,
            DurationFormat.MS_DIGITAL
        ).toString(),
        fontWeight = FontWeight.Light,
        fontSize = 75.sp
    )
    Text(
        modifier = Modifier.alpha(0.8f),
        style = MaterialTheme.typography.bodyLarge,
        text = "Practice time",
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentPracticingItem(
    item: ActiveSessionSectionListItemUiState?,
) {
    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        Surface(
            modifier = Modifier.animateContentSize(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
            shadowElevation = 1.dp
        ) {
            if (item == null) return@Surface

            AnimatedContent(
                targetState = item.libraryItem.name,
                label = "currentPracticingItem",
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.large,
                            vertical = MaterialTheme.spacing.medium
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        modifier = Modifier.alpha(0.8f),
                        text = getDurationString(
                            item.duration,
                            DurationFormat.MS_DIGITAL
                        ).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
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

    LazyColumn(
        state = listState,
        modifier = modifier
            .fadingEdge(listState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        contentPadding = PaddingValues(
            top = MaterialTheme.spacing.small,
            bottom = additionalBottomContentPadding + MaterialTheme.spacing.small
        ),
    ) {

        items(
            items = uiState.sections,
            key = { sectionElement -> sectionElement.id },
        ) { sectionElement ->
            SectionListElement(
                Modifier.animateItemPlacement(),
                sectionElement,
                onSectionDeleted = onSectionDeleted
            )
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

@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    sectionElement: ActiveSessionSectionListItemUiState,
    onSectionDeleted: (ActiveSessionSectionListItemUiState) -> Unit,
) {
    Surface(
        // Surface for setting shape + padding of list item
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),  // margin around list
        shape = MaterialTheme.shapes.medium,
    ) {
        SwipeToDeleteContainer(onDeleted = { onSectionDeleted(sectionElement) }) {
            Surface(    // Surface for setting the Color of the List item
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.medium
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = sectionElement.libraryItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        modifier = Modifier.alpha(0.8f),
                        text = getDurationString(
                            sectionElement.duration,
                            DurationFormat.MS_DIGITAL
                        ).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
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