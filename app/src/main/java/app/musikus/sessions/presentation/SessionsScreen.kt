/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde, Michael Prommersberger
 */

package app.musikus.sessions.presentation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
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
import app.musikus.core.presentation.components.MusikusShowcaseDialog
import app.musikus.core.presentation.components.Selectable
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationString
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import com.joco.showcase.sequence.SequenceShowcase
import com.joco.showcase.sequence.rememberSequenceShowcaseState
import com.joco.showcaseview.ShowcaseAlignment
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * The order in which UI items of this screen are introduced by app intro dialogs.
 */
private enum class IntroOrder(val index: Int) {
    FAB(0),
    MONTH_HEADER(1),
    SESSION_CARD(2),
}

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
    val sequenceShowcaseState = rememberSequenceShowcaseState()
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

    /**
     * When a session is finished, scroll to the top
     */
    LaunchedEffect(mainUiState.isSessionRunning) {
        if (!mainUiState.isSessionRunning) {
            delay(300.milliseconds)
            sessionsListState.animateScrollToItem(0)
        }
        sequenceShowcaseState.start(index = uiState.appIntroDialogIndex) // TODO add logic for starting showcase
    }

    val context = LocalContext.current

    SequenceShowcase(state = sequenceShowcaseState) {
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
                        },
                    modifier = Modifier.sequenceShowcaseTarget(
                        index = IntroOrder.FAB.index,
                        content = {
                            MusikusShowcaseDialog(
                                targetRect = it,
                                text = UiText.StringResource(resId = R.string.sessions_app_intro_fab),
                                onClick = {
                                    eventHandler(SessionsUiEvent.UpdateAppIntroDialogIndex(IntroOrder.FAB.index + 1))
                                    sequenceShowcaseState.next()
                                }
                            )
                        }
                    )
                )
            },
            topBar = {
                // Main top bar
                MusikusTopBar(
                    isTopLevel = true,
                    title = UiText.StringResource(R.string.sessions_title),
                    scrollBehavior = scrollBehavior,
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
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
                    state = sessionsListState,
                ) {
                    contentUiState.monthData.forEach { monthDatum ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                            ) {
                                MonthHeader(
                                    month = monthDatum.month,
                                    onClickHandler = {
                                        eventHandler(SessionsUiEvent.MonthHeaderPressed(monthDatum.specificMonth))
                                    },
                                    modifier = Modifier.sequenceShowcaseTarget(
                                        index = IntroOrder.MONTH_HEADER.index,
                                        alignment = ShowcaseAlignment.CenterHorizontal,
                                        content = {
                                            MusikusShowcaseDialog(
                                                targetRect = it,
                                                text = UiText.StringResource(resId = R.string.sessions_app_intro_month_button),
                                                onClick = {
                                                    eventHandler(SessionsUiEvent.UpdateAppIntroDialogIndex(
                                                        IntroOrder.MONTH_HEADER.index + 1))
                                                    sequenceShowcaseState.next()
                                                }
                                            )
                                        }
                                    )
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
                                    // Show the App intro only for the first session of the last day
                                    if (session == monthDatum.dayData.first().sessions.first()) {
                                        SessionCard(
                                            modifier = Modifier.sequenceShowcaseTarget(
                                                index = IntroOrder.SESSION_CARD.index,
                                                alignment = ShowcaseAlignment.CenterHorizontal,
                                                content = {
                                                    MusikusShowcaseDialog(
                                                        targetRect = it,
                                                        text = UiText.StringResource(resId = R.string.sessions_app_intro_card),
                                                        onClick = {
                                                            eventHandler(SessionsUiEvent.UpdateAppIntroDialogIndex(
                                                                IntroOrder.SESSION_CARD.index + 1))
                                                            sequenceShowcaseState.next() }
                                                    )
                                                }
                                            ),
                                            sessionWithSectionsWithLibraryItems = session)
                                    } else {
                                        SessionCard(
                                            sessionWithSectionsWithLibraryItems = session
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // extra large spacer as footer for clearing the fab button
                    item {
                        Spacer(modifier = Modifier.height(56.dp))
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
}

@Composable
fun MonthHeader(
    modifier: Modifier = Modifier,
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
            modifier = modifier,
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
