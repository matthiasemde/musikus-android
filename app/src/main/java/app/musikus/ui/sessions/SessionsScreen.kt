/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 * Additions and modifications, author Matthias Emde
 */

package app.musikus.ui.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.ui.MainUiEvent
import app.musikus.ui.MainUiEventHandler
import app.musikus.ui.MainUiState
import app.musikus.ui.Screen
import app.musikus.ui.components.ActionBar
import app.musikus.ui.components.CommonMenuSelections
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.components.MainMenu
import app.musikus.ui.components.Selectable
import app.musikus.ui.theme.spacing
import app.musikus.utils.DurationString
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText
import java.util.UUID

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel = hiltViewModel(),
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    navigateTo: (Screen) -> Unit,
    onSessionEdit: (sessionId: UUID) -> Unit,
    onSessionStart: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler: SessionsUiEventHandler = viewModel::onUiEvent

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val sessionsListState = rememberLazyListState()
    // The FAB is initially expanded. Once the first visible item is past the first item we
    // collapse the FAB. We use a remembered derived state to minimize unnecessary recompositions.
    val fabExpanded by remember {
        derivedStateOf {
            sessionsListState.firstVisibleItemIndex == 0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "new session"
                    )
                },
                text = { Text(text = "Start Session") },
                onClick = {
                          onSessionStart()
                },
                expanded = fabExpanded,
            )
        },
        topBar = {
            val mainMenuUiState = mainUiState.menuUiState
            val topBarUiState = uiState.topBarUiState
            LargeTopAppBar(
                title = { Text(text = topBarUiState.title) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { mainEventHandler(MainUiEvent.ShowMainMenu) } ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainMenuUiState.show,
                            onDismissHandler = { mainEventHandler(MainUiEvent.HideMainMenu) },
                            onSelectionHandler = { commonSelection ->
                                mainEventHandler(MainUiEvent.HideMainMenu)

                                when (commonSelection) {
                                    CommonMenuSelections.SETTINGS -> { navigateTo(Screen.Settings) }
                                }
                            },
                            uniqueMenuItems = { }
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = { eventHandler(SessionsUiEvent.ClearActionMode) },
                    onEditHandler = { eventHandler(SessionsUiEvent.EditButtonPressed(onSessionEdit)) },
                    onDeleteHandler = { eventHandler(SessionsUiEvent.DeleteButtonPressed) }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = uiState.contentUiState

            // Session list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.large,
                    end = MaterialTheme.spacing.large,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 56.dp,
                ),
                state = sessionsListState,
            ) {
                contentUiState.monthData.forEach { monthDatum ->
                    item {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                        ) {
                            MonthHeader(
                                month = monthDatum.month,
                                onClickHandler = {
                                    eventHandler(SessionsUiEvent.MonthHeaderPressed(monthDatum.specificMonth))
                                }
                            )
                        }
                    }
                    monthDatum.dayData.forEach { dayDatum ->
                        item {
                            DayHeader(
                                date = dayDatum.date,
                                totalPracticeDuration = dayDatum.totalPracticeDuration
                            )
                        }
                        items(
                            items = dayDatum.sessions,
                            key = { it.session.id }
                        ) { session ->
                            Selectable(
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                                selected = session.session.id in contentUiState.selectedSessions,
                                onShortClick = {
                                    eventHandler(SessionsUiEvent.SessionPressed(session.session.id, longClick = false))
                                },
                                onLongClick = {
                                    eventHandler(SessionsUiEvent.SessionPressed(session.session.id, longClick = true))
                                },
                            ) {
                                SessionCard(sessionWithSectionsWithLibraryItems = session)
                            }
                        }
                    }
                }
            }

            // Delete Dialog
            val deleteDialogUiState = uiState.deleteDialogUiState

            if(deleteDialogUiState != null) {
                DeleteConfirmationBottomSheet(
                    explanation = UiText.DynamicString("Delete sessions? Any progress you made towards your goals during these sessions will be lost."),
                    confirmationText = UiText.DynamicString("Delete forever (${deleteDialogUiState.numberOfSelections})"),
                    confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
                    onDismiss = { eventHandler(SessionsUiEvent.DeleteDialogDismissed) },
                    onConfirm = {
                        eventHandler(SessionsUiEvent.DeleteDialogConfirmed)
                        mainEventHandler(MainUiEvent.ShowSnackbar(
                            message = "Deleted ${deleteDialogUiState.numberOfSelections} sessions",
                            onUndo = { eventHandler(SessionsUiEvent.UndoButtonPressed) }
                        ))
                    }
                )
            }
        }
    )
}

@Composable
fun MonthHeader(
    month: String,
    onClickHandler: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onClickHandler
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DayHeader(
    date: String,
    totalPracticeDuration: DurationString,
) {
    Row(
        modifier = Modifier
            .padding(vertical = MaterialTheme.spacing.small)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = totalPracticeDuration,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}