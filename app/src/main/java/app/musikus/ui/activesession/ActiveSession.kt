/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package app.musikus.ui.activesession

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.ui.activesession.metronome.Metronome
import app.musikus.ui.activesession.recorder.Recorder
import app.musikus.ui.components.SwipeToDeleteContainer
import app.musikus.ui.components.fadingEdge
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import kotlinx.coroutines.launch


const val CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN = 0.7f

@OptIn(ExperimentalFoundationApi::class)
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

    Scaffold { contentPadding ->
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
                modifier = Modifier.fillMaxSize()
            ) {

                /** ------------------- Area above extended Cards ------------------- */
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1 - CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN)
                        .padding(horizontal = MaterialTheme.spacing.medium)
                ){
                    HeaderBar(uiState, uiEvent)
                    Spacer(modifier = Modifier.weight(1f))
                    PracticeTimer(uiState)
                    Spacer(modifier = Modifier.weight(1f))
                    CurrentPracticingItem(uiState = uiState)
                }

                /** ------------------- Remaining area ------------------- */
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN)
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.medium)
                ) {
                    SectionsList(
                        modifier = Modifier.weight(1f),
                        uiState = uiState,
                        onSectionDeleted = { uiEvent(ActiveSessionUIEvent.DeleteSection(it)) }
                    )
                    // prevent section list items to hide behind peek'ed Cards
                    Spacer(
                        Modifier.height(
                            MaterialTheme.dimensions.bottomButtonsPagerHeight +
                            MaterialTheme.dimensions.cardPeekHeight
                        )
                    )
                }

            }

            /**
             *
             *  ------------------- Bottom Draggable Cards Layout -------------------
             *
             */

            val pageCount = 3
            val animationScope = rememberCoroutineScope()

            // DraggableAnchorState initialization. Each page gets own state.
            val stateListDraggableCards = getDraggableStateList(pageCount = pageCount)
            val anchorStates = remember { stateListDraggableCards }

            DraggableCardsPagerLayout(
                pageCount = pageCount,
                anchorStates = anchorStates,
                scrollStates = getScrollableStateList(pageCount = pageCount),
                pages = { pageIndex ->
                    when(pageIndex) {
                        0 -> DraggableCardPage(
                            title = "Library",
                            isExpandable = true,
                            header = {
                                val calculatedHeight = getDynamicHeaderHeight(anchorStates[pageIndex])
                                LibraryHeader(
                                    modifier = Modifier.defaultMinSize(minHeight = calculatedHeight),
                                    uiState = uiState.libraryUiState,
                                    onFolderIconClicked = {
                                        uiEvent(ActiveSessionUIEvent.ChangeFolderDisplayed(it))
                                    },
                                    extendCardIfCollapsed = {
                                        if (anchorStates[pageIndex].currentValue != DragValueY.Collapsed) {
                                            return@LibraryHeader
                                        }
                                        animationScope.launch {
                                            anchorStates[pageIndex].animateTo(
                                                DragValueY.Normal
                                            )
                                        }
                                    },
                                    isCardCollapsed = anchorStates[pageIndex].currentValue == DragValueY.Collapsed
                                )
                            },
                            content = {
                                LibraryList(
                                    uiState = uiState.libraryUiState,
                                    onLibraryItemClicked = {
                                        uiEvent(ActiveSessionUIEvent.StartNewSection(it))
                                        animationScope.launch {
                                            anchorStates[pageIndex].animateTo(
                                                DragValueY.Normal
                                            )
                                        }
                                    }
                                )
                            }
                        )
                        1-> DraggableCardPage(
                            title = "Recorder",
                            isExpandable = false,
                            content = { Recorder() },
                        )
                        2-> DraggableCardPage(
                            title = "Metronome",
                            isExpandable = false,
                            content = { Metronome() },
                        )
                        else -> DraggableCardPage(
                            title = "unknown",
                            isExpandable = false,
                            content = {},
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun HeaderBar(
    uiState: ActiveSessionUiState,
    uiEvent: (ActiveSessionUIEvent) -> Unit
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        OutlinedButton(
            enabled = uiState.sections.isNotEmpty(),
            onClick = { uiEvent(ActiveSessionUIEvent.TogglePause)  }
        ) {
            Text(text = "Pause")
        }
        TextButton(
            enabled = uiState.sections.isNotEmpty(),
            onClick = {
                uiEvent(ActiveSessionUIEvent.StopSession)

            }) {
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
        text = getDurationString(uiState.totalSessionDuration, DurationFormat.HMS_DIGITAL).toString(),
        fontWeight = FontWeight.Bold
    )
    Text(text = "Practice time")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentPracticingItem(
    uiState: ActiveSessionUiState
) {
    AnimatedVisibility(
        visible = uiState.sections.isNotEmpty(),
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        val firstSection = uiState.sections.first()
        Surface(
            modifier = Modifier.animateContentSize(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
            shadowElevation = 1.dp
        ) {

            AnimatedContent(
                targetState = firstSection.name,
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = getDurationString(
                            firstSection.duration,
                            DurationFormat.HMS_DIGITAL
                        ).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionsList(
    uiState: ActiveSessionUiState,
    modifier: Modifier = Modifier,
    onSectionDeleted: (SectionListItem) -> Unit = {}
) {
    if (uiState.sections.isEmpty()) {
        Box (modifier = modifier) {
            Text(text = "Quotes",)
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
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.small),
    ) {

        items(
            items = uiState.sections.drop(1),
            key = { sectionElement -> sectionElement.startTimeStamp },
        ) { sectionElement ->
            SectionListElement(
                Modifier.animateItemPlacement(),
                sectionElement,
                onSectionDeleted = onSectionDeleted
            )
        }

    }
}

@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    sectionElement: SectionListItem,
    onSectionDeleted: (SectionListItem) -> Unit
) {
    Surface(   // Surface for setting shape + padding of list item
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
                        text = sectionElement.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = getDurationString(
                            sectionElement.duration,
                            DurationFormat.HMS_DIGITAL
                        ).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


@Composable
private fun LibraryHeader(
    modifier: Modifier = Modifier,
    uiState: LibraryCardUiState,
    onFolderIconClicked: (LibraryFolder?) -> Unit,
    extendCardIfCollapsed: () -> Unit,
    isCardCollapsed: Boolean
) {
    // Header Folder List
    LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small),
    ) {

        item {
            FilterChip(
                onClick = {
                    onFolderIconClicked(null)
                    extendCardIfCollapsed()
                },
                label = {
                    Text("no folder")
                },
                selected = uiState.selectedFolder == null && !isCardCollapsed
            )
        }

        items(uiState.foldersWithItems) { folder ->
            Row {
                FilterChip(
                    onClick = {
                        onFolderIconClicked(folder.folder)
                        extendCardIfCollapsed()
                    },
                    label = { Text(folder.folder.name) },
                    selected = folder.folder == uiState.selectedFolder && !isCardCollapsed
                )
            }
        }
    }
}

@Composable
private fun LibraryList(
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // show folder items or if folderId could not be found, show root Items
        val shownItems =
            uiState.foldersWithItems.find { it.folder == uiState.selectedFolder }?.items
                ?: uiState.rootItems
        for (item in shownItems) {
            LibraryUiItem(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.spacing.small,
                    horizontal = MaterialTheme.spacing.large
                ),
                item = item,
                selected = false,
                onShortClick = {
                    onLibraryItemClicked(item)
                },
                onLongClick = { /*TODO*/ })
        }
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



/**
 * Returns a offset-dependent height which gradually increases from 0dp to the maximum usable height
 * when a card is in Peek state.
 * Can be used for the header of a DraggableCardPage. Use it with the defaultMinSize modifier.
 *
 * @param anchorState the state of the DraggableCardPage
 */
@Composable
private fun getDynamicHeaderHeight(
    anchorState: AnchoredDraggableState<DragValueY>
) : Dp {
    val fraction = getCurrentOffsetFraction(state = anchorState)
    val peekHeightContent =
        MaterialTheme.dimensions.cardPeekHeight -
                MaterialTheme.dimensions.cardHandleHeight
    val height = if (anchorState.targetValue != DragValueY.Full){
        (fraction * peekHeightContent.value).dp
    } else 0.dp

    return height
}