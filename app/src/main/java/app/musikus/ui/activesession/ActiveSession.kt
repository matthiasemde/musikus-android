/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import app.musikus.ui.activesession.metronome.MetronomeBody
import app.musikus.ui.activesession.metronome.MetronomeHeader
import app.musikus.ui.activesession.recorder.RecorderBody
import app.musikus.ui.activesession.recorder.RecorderHeader
import app.musikus.ui.components.SwipeToDeleteContainer
import app.musikus.ui.components.fadingEdge
import app.musikus.ui.library.DialogMode
import app.musikus.ui.library.LibraryItemDialog
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.theme.dimensions
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString
import java.util.UUID


const val CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN = 0.7f

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
                    CurrentPracticingItem(sections = uiState.sections)
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
                        onSectionDeleted = { uiEvent(ActiveSessionUIEvent.DeleteSection(it.id)) }
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

            DraggableCardsPagerLayout(
                pages = listOf(
                    DraggableCardPageData(
                        title = "Library",
                        isExpandable = true,
                        fabAction = {
                            uiEvent(ActiveSessionUIEvent.CreateNewLibraryItem(
                                uiState.libraryUiState.selectedFolder?.id))
                        },
                        header = { isCollapsed, onExpandToNormal ->
                            LibraryHeader(
                                modifier = Modifier.fillMaxSize(),
                                uiState = uiState.libraryUiState,
                                onFolderClicked = {
                                    uiEvent(ActiveSessionUIEvent.ChangeFolderDisplayed(it))
                                },
                                shouldShowBadge = { folderId ->
                                    // look if current item is inside of folder
                                    uiState.sections.firstOrNull()?.let { activeSection ->
                                        folderId == activeSection.libraryItem.libraryFolderId
                                    } ?: false

                                },
                                isCardCollapsed = isCollapsed,
                                onExtendCardToNormal = onExpandToNormal
                            )
                        },
                        body = { modifier, scrollState, onShrinkToNormal ->
                            LibraryList(
                                modifier = modifier,
                                uiState = uiState.libraryUiState,
                                cardScrollState = scrollState,
                                onLibraryItemClicked = {
                                    uiEvent(ActiveSessionUIEvent.StartNewSection(it))
                                    onShrinkToNormal()
                                },
                                currentPracticedItem = uiState.sections.firstOrNull()?.libraryItem
                            )
                        }
                    ),
                    DraggableCardPageData(
                        title = "Recorder",
                        isExpandable = true,
                        header = { _, _ -> RecorderHeader() },
                        body = { modifier, _, _ -> RecorderBody(modifier = modifier) },
                    ),
                    DraggableCardPageData(
                        title = "Metronome",
                        isExpandable = false,
                        header = { _, onExpandToNormal -> MetronomeHeader(
                            /** change Card height */
                            onTextClicked = onExpandToNormal
                        ) },
                        body = { modifier, _, _ -> MetronomeBody(modifier = modifier) },
                    )
                )
            )
        }


        if(uiState.isPaused) {
            PauseDialog(uiState, uiEvent)
        }

        uiState.newLibraryItemData?.let { editData ->
            LibraryItemDialog(
                mode = DialogMode.ADD,
                folders = uiState.libraryUiState.foldersWithItems.map { it.folder },
                itemData = editData,
                onNameChange = { uiEvent(ActiveSessionUIEvent.NewLibraryItemNameChanged(it)) },
                onColorIndexChange = {uiEvent(ActiveSessionUIEvent.NewLibraryItemColorChanged(it))},
                onSelectedFolderIdChange = {uiEvent(ActiveSessionUIEvent.NewLibraryItemFolderChanged(it))},
                onConfirmHandler = { /*TODO*/ },
                onDismissHandler = { uiEvent(ActiveSessionUIEvent.NewLibraryItemDialogDismissed) }
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
    sections: List<SectionListItemUiState>
) {
    AnimatedVisibility(
        visible = sections.isNotEmpty(),
        enter = expandVertically() + fadeIn(animationSpec = keyframes { durationMillis = 200 }),
    ) {
        val firstSection = sections.first()
        Surface(
            modifier = Modifier.animateContentSize(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
            shadowElevation = 1.dp
        ) {

            AnimatedContent(
                targetState = firstSection.libraryItem.name,
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
    onSectionDeleted: (SectionListItemUiState) -> Unit = {}
) {
    if (uiState.sections.isEmpty()) {
        Box (modifier = modifier) {
            Text(text = "Quotes")
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
            key = { sectionElement -> sectionElement.id },
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
    sectionElement: SectionListItemUiState,
    onSectionDeleted: (SectionListItemUiState) -> Unit
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
                        text = sectionElement.libraryItem.name,
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
    shouldShowBadge: (UUID?) -> Boolean,
    onFolderClicked: (LibraryFolder?) -> Unit,
    onExtendCardToNormal: () -> Unit,
    isCardCollapsed: Boolean
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

        item {
            LibraryFolderElement(
                folder = null,
                onClick = {
                    onFolderClicked(null)
                    if(isCardCollapsed) onExtendCardToNormal()
                },
                isSelected = uiState.selectedFolder == null && !isCardCollapsed,
                showBadge = shouldShowBadge(null)
            )
        }

        items(uiState.foldersWithItems) { folder ->
            LibraryFolderElement(folder = folder.folder,
                onClick = {
                    onFolderClicked(folder.folder)
                    if(isCardCollapsed) onExtendCardToNormal()
                },
                isSelected = folder.folder == uiState.selectedFolder && !isCardCollapsed,
                showBadge = shouldShowBadge(folder.folder.id)
            )
        }
    }
}


@Composable
private fun LibraryFolderElement(
    folder: LibraryFolder?,
    showBadge: Boolean = false ,
    onClick: (LibraryFolder?) -> Unit,
    isSelected: Boolean,
) {
    val color by animateColorAsState(targetValue =
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick(folder) },
            color = color
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
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
}


@Composable
private fun LibraryList(
    modifier: Modifier = Modifier,
    uiState: LibraryCardUiState,
    onLibraryItemClicked: (LibraryItem) -> Unit,
    cardScrollState: ScrollState,
    currentPracticedItem: LibraryItem?
) {
//    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium)) TODO
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fadingEdge(cardScrollState)
    ) {
        // show folder items or if folderId could not be found, show root Items
        val shownItems =
            uiState.foldersWithItems.find { it.folder == uiState.selectedFolder }?.items
                ?: uiState.rootItems

        for (item in shownItems) {
            LibraryUiItem(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.spacing.small,
                    horizontal = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                ),
                item = item,
                selected = false,
                onShortClick = {
                    onLibraryItemClicked(item)
                },
                onLongClick = { /*TODO*/ },
                compact = true,
                enabled = currentPracticedItem != item
            )
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun getDynamicHeaderHeight(
    anchorState: AnchoredDraggableState<DragValueY>
) : Dp {
    val fraction = getCurrentOffsetFraction(state = anchorState)
    val peekHeightContent = MaterialTheme.dimensions.cardPeekContentHeight
    val height = if (anchorState.targetValue != DragValueY.Full){
        (fraction * peekHeightContent.value).dp
    } else 0.dp

    return height
}