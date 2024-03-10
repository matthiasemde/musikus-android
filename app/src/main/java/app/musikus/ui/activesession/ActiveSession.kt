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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlin.time.Duration


const val CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN = 0.7f

/**
 * Actions that can be triggered by the Notification
 */
enum class ActiveSessionActions {
    OPEN, PAUSE, FINISH, METRONOME, RECORDER
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ActiveSession(
    viewModel: ActiveSessionViewModel = hiltViewModel(),
    deepLinkArgument: String?,
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    Surface(
        color = MaterialTheme.colorScheme.background,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .weight(1f)
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
                    ) {
                        HeaderBar(uiState, eventHandler, navigateUp)
                        Spacer(modifier = Modifier.weight(1f))
                        PracticeTimer(uiState)
                        Spacer(modifier = Modifier.weight(1f))
                        CurrentPracticingItem(item = uiState.runningSection)
                    }

                    /** ------------------- Remaining area ------------------- */
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN)
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.medium)
                    ) {
                        SectionsList(
                            modifier = Modifier.weight(1f),
                            uiState = uiState,
                            onSectionDeleted = { eventHandler(ActiveSessionUiEvent.DeleteSection(it.id)) }
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
                if (uiState.cardUiStates.isEmpty()) return@Box // TODO take another look at this

                DraggableCardsPagerLayout(
                    cardUiStates = uiState.cardUiStates,
                    cardHeaderComposable = { headerUiState, cardState, eventHandler ->
                        ActiveSessionDraggableCardHeader(headerUiState, cardState, eventHandler)
                    },
                    cardBodyComposable = { bodyState, cardState, eventHandler ->
                        ActiveSessionDraggableCardBody(bodyState, cardState, eventHandler)
                    },
                    eventHandler = { event ->
                        when (event) {
                            is ActiveSessionUiEvent -> eventHandler(event)
                            // currently event handler can't differentiate between different fabs from
                            // different cards... sounds like a future me problem
                            is DraggableCardUiEvent.FabAction -> eventHandler(ActiveSessionUiEvent.CreateNewLibraryItem)
                            else -> throw IllegalArgumentException("Unknown event: $event")
                        }
                    },
                    initialCardIndex = when (deepLinkArgument) {
                        ActiveSessionActions.RECORDER.name -> 1
                        ActiveSessionActions.METRONOME.name -> 2
                        else -> 0
                    }
                )
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

            // Background behind the nav bar
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
            ) {
                Box(Modifier.fillMaxSize()) // necessary for the background to be drawn
            }
        }
    }

    if (deepLinkArgument == ActiveSessionActions.FINISH.name) {
        eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
    }
}

@Composable
private fun HeaderBar(
    uiState: ActiveSessionUiState,
    eventHandler: ActiveSessionUiEventHandler,
    navigateUp: () -> Unit
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Row {
            IconButton(
                onClick = { navigateUp() }
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            IconButton(
                onClick = { eventHandler(ActiveSessionUiEvent.ShowDiscardSessionDialog) }
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            OutlinedButton(
                enabled = uiState.sections.isNotEmpty(),
                onClick = { eventHandler(ActiveSessionUiEvent.TogglePause)  }
            ) {
                Text(text = "Pause")
            }
        }
        TextButton(
            enabled = uiState.sections.isNotEmpty(),
            onClick = {
                eventHandler(ActiveSessionUiEvent.ShowFinishDialog)
            }
        ) {
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
    item: ActiveSessionSectionListItemUiState?
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
            if(item == null) return@Surface

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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.width(MaterialTheme.spacing.small))

                    Text(
                        text = getDurationString(
                            item.duration,
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
    onSectionDeleted: (ActiveSessionSectionListItemUiState) -> Unit = {}
) {
    if (uiState.sections.isEmpty()) {
        Box (modifier = modifier) {
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
    sectionElement: ActiveSessionSectionListItemUiState,
    onSectionDeleted: (ActiveSessionSectionListItemUiState) -> Unit
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveSessionDraggableCardHeader(
    uiState: ActiveSessionDraggableCardHeaderUiState,
    cardState: DraggableCardLocalState,
    eventHandler: DraggableCardUiEventHandler
) {
    when(uiState) {
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
    eventHandler: DraggableCardUiEventHandler
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
    eventHandler: DraggableCardUiEventHandler
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
                    if(isCardCollapsed) eventHandler(DraggableCardUiEvent.ResizeCard(DragValueY.Normal))
                },
                isSelected = folder?.id == uiState.selectedFolderId && !isCardCollapsed,
                showBadge = uiState.activeFolderId?.let { it.value == folder?.id} ?: false
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
    eventHandler: DraggableCardUiEventHandler
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fadingEdge(cardScrollState)
    ) {
        for (item in uiState.items) {
            LibraryUiItem(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.spacing.small,
                    horizontal = MaterialTheme.spacing.medium + MaterialTheme.spacing.small,
                ),
                item = item,
                selected = false,
                onShortClick = {
                    eventHandler(ActiveSessionUiEvent.SelectItem(item))
                    eventHandler(DraggableCardUiEvent.ResizeCard(DragValueY.Normal))
                },
                onLongClick = { },
                compact = true,
                enabled = uiState.activeItemId != item.id
            )
        }
    }
}


@Composable
private fun PauseDialog(
    ongoingBreakDuration: Duration,
    eventHandler: ActiveSessionUiEventHandler
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
                            DurationFormat.HMS_DIGITAL
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
    onConfirm: () -> Unit
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