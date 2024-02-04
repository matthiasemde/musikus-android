/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.ui.activesession

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import app.musikus.ui.library.LibraryUiItem
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDurationString


const val CARD_HEIGHT_EXTENDED_FRACTION_OF_SCREEN = 0.7f
val CARD_HEIGHT_NORMAL = 300.dp
val CARD_HEIGHT_PEEK = 100.dp
const val HEIGHT_BOTTOM_BUTTONS_DP = 60

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
                        uiState = uiState)
                    // prevent section list items to hide behind peek'ed Cards
                    Spacer(
                        Modifier.height(
                            HEIGHT_BOTTOM_BUTTONS_DP.dp + CARD_HEIGHT_PEEK
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
                pageCount = 3,
                pages = { pageIndex ->
                    when(pageIndex) {
                        0 -> DraggableCardPage(
                            title = "Library",
                            isExpandable = true,
                            content = {
                                LibraryList(
                                    uiState = uiState.libraryUiState,
                                    onLibraryItemClicked = {
                                        uiEvent(ActiveSessionUIEvent.StartNewSection(it))
                                    }
                                )
                            },
                            header = {
                                LibraryHeader(
                                    uiState = uiState.libraryUiState,
                                    onFolderIconClicked = {
                                        uiEvent(ActiveSessionUIEvent.ChangeFolderDisplayed(it))
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
    if (uiState.sections.isNotEmpty()) {

        val firstSection = uiState.sections.first()
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50)
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
                    text = firstSection.name,
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


@Composable
private fun LibraryHeader(
    modifier: Modifier = Modifier,
    uiState: LibraryCardUiState,
    onFolderIconClicked: (LibraryFolder?) -> Unit
) {
    // Header Folder List
    LazyRow(
        modifier = modifier
            .padding(top = MaterialTheme.spacing.small)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small),
    ) {

        item {
            FilterChip(
                onClick = { onFolderIconClicked(null) },
                label = {
                    Text("no folder")
                },
                selected = uiState.selectedFolder == null
            )
        }

        items(uiState.foldersWithItems) { folder ->
            Row {
                FilterChip(
                    onClick = {
                        onFolderIconClicked(folder.folder)
                    },
                    label = { Text(folder.folder.name) },
                    selected = folder.folder == uiState.selectedFolder
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionsList(
    uiState: ActiveSessionUiState,
    modifier: Modifier = Modifier
) {
    if (uiState.sections.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            contentPadding = PaddingValues(vertical = MaterialTheme.spacing.small),
        ) {

            items(
                items = uiState.sections.drop(1),
                key = { (sectionElement) -> sectionElement },
            ) { sectionElement ->
                SectionListElement(Modifier.animateItemPlacement(), sectionElement)
            }

        }
    } else {
        Box (
            modifier = modifier
        ) {
            Text(
                text = "Quotes",
            )
        }
    }
}

@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    sectionElement: SectionListItem
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row (
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small
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