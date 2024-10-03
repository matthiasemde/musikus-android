/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.presentation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.MainUiState
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.ActionBar
import app.musikus.core.presentation.components.DeleteConfirmationBottomSheet
import app.musikus.core.presentation.components.Selectable
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationString
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import java.util.UUID

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun SessionsScreen(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    viewModel: SessionsViewModel = hiltViewModel(),
    navigateTo: (Screen) -> Unit,
    navigateUp: () -> Unit,
    bottomBarHeight: Dp,
    onSessionEdit: (sessionId: UUID) -> Unit,
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

    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = bottomBarHeight),
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = {
                    if (mainUiState.isSessionRunning) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.sessions_screen_fab_resume)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.sessions_screen_fab)
                        )
                    }
                },
                text = {
                    Text(
                        text = stringResource(
                            id = if (mainUiState.isSessionRunning) {
                                R.string.sessions_screen_fab_resume
                            } else {
                                R.string.sessions_screen_fab
                            }
                        )
                    )
                },
                onClick = { navigateTo(Screen.ActiveSession()) },
                expanded = fabExpanded,
                containerColor =
                if (mainUiState.isSessionRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
        },
        topBar = {
            // Main top bar
            MusikusTopBar(
                isTopLevel = true,
                title = UiText.StringResource(R.string.sessions_title),
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp,
                openMainMenu = { mainEventHandler(MainUiEvent.OpenMainMenu) }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if (actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = { eventHandler(SessionsUiEvent.ClearActionMode) },
                    onEditHandler = {
                        Toast.makeText(context, context.getString(R.string.core_coming_soon), Toast.LENGTH_SHORT).show()
                        eventHandler(SessionsUiEvent.EditButtonPressed(onSessionEdit)) // TODO
                    },
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
                        Row(
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
                                    eventHandler(
                                        SessionsUiEvent.SessionPressed(
                                            session.session.id,
                                            longClick = false
                                        )
                                    )
                                },
                                onLongClick = {
                                    eventHandler(
                                        SessionsUiEvent.SessionPressed(
                                            session.session.id,
                                            longClick = true
                                        )
                                    )
                                },
                            ) {
                                SessionCard(sessionWithSectionsWithLibraryItems = session)
                            }
                        }
                    }
                }
            }

            // Show hint if there are no sessions
            if (contentUiState.showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.sessions_screen_hint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            /**
             *  ---------------------- Dialogs ----------------------
             */

            // Delete session dialog
            val deleteDialogUiState = uiState.deleteDialogUiState

            if (deleteDialogUiState != null) {
                val snackbarMessage = stringResource(id = R.string.sessions_screen_snackbar_deleted)

                DeleteConfirmationBottomSheet(
                    explanation = UiText.PluralResource(
                        resId = R.plurals.sessions_screen_delete_session_dialog_explanation,
                        quantity = deleteDialogUiState.numberOfSelections,
                        deleteDialogUiState.numberOfSelections
                    ),
                    confirmationText = UiText.StringResource(
                        resId = R.string.sessions_screen_delete_session_dialog_confirm,
                        deleteDialogUiState.numberOfSelections
                    ),
                    confirmationIcon = UiIcon.DynamicIcon(Icons.Default.Delete),
                    onDismiss = { eventHandler(SessionsUiEvent.DeleteDialogDismissed) },
                    onConfirm = {
                        eventHandler(SessionsUiEvent.DeleteDialogConfirmed)
                        mainEventHandler(
                            MainUiEvent.ShowSnackbar(
                                message = snackbarMessage,
                                onUndo = { eventHandler(SessionsUiEvent.UndoButtonPressed) }
                            )
                        )
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
