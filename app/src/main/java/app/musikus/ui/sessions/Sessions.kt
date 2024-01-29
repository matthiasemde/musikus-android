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
import app.musikus.shared.ActionBar
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.Selectable
import app.musikus.shared.ThemeMenu
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.ui.theme.spacing
import java.util.UUID

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun Sessions(
    viewModel: SessionsViewModel = hiltViewModel(),
    mainUiState: MainUiState,
    mainEventHandler: (event: MainUIEvent) -> Unit,
    onSessionEdit: (sessionId: UUID) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val sessionsListState = rememberLazyListState()
    // The FAB is initially expanded. Once the first visible item is past the first item we
    // collapse the FAB. We use a remembered derived state to minimize unnecessary recompositions.
    val fabExpanded by remember {
        derivedStateOf {
            sessionsListState.firstVisibleItemIndex == 0
        }
    }

    @Composable
    fun MusikusScaffold(content: @Composable (PaddingValues) -> Unit) {
        Scaffold (
            content = content,
        )
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
                    IconButton(onClick = { mainEventHandler(MainUIEvent.ShowMainMenu) } ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainMenuUiState.show,
                            onDismissHandler = { mainEventHandler(MainUIEvent.HideMainMenu) },
                            onSelectionHandler = { commonSelection ->
                                mainEventHandler(MainUIEvent.HideMainMenu)

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainEventHandler(MainUIEvent.ShowThemeSubMenu)
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainEventHandler(MainUIEvent.ShowExportImportDialog)
                                    }
                                }
                            },
                            uniqueMenuItems = { }
                        )
                        ThemeMenu(
                            expanded = mainMenuUiState.showThemeSubMenu,
                            currentTheme = mainUiState.activeTheme,
                            onDismissHandler = { mainEventHandler(MainUIEvent.HideThemeSubMenu) },
                            onSelectionHandler = { theme ->
                                mainEventHandler(MainUIEvent.HideThemeSubMenu)
                                mainEventHandler(MainUIEvent.SetTheme(theme))
                            }
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = viewModel::clearActionMode,
                    onEditHandler = {
                        viewModel.onEditAction(onSessionEdit)
                    },
                    onDeleteHandler = {
                        viewModel.onDeleteAction()
                        mainEventHandler(MainUIEvent.ShowSnackbar(
                            message = "Deleted ${actionModeUiState.numberOfSelections} sessions",
                            onUndo = viewModel::onRestoreAction
                        ))
                    }
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
                                    viewModel.onMonthHeaderClicked(monthDatum.specificMonth)
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
                                    viewModel.onSessionClicked(session.session.id)
                                },
                                onLongClick = {
                                    viewModel.onSessionClicked(session.session.id, longClick = true)
                                },
                            ) {
                                SessionCard(sessionWithSectionsWithLibraryItems = session)
                            }
                        }
                    }
                }
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
    totalPracticeDuration: String,
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

//
//    // check if session is running every second to respond the fab design to it
//    override fun onResume() {
//        super.onResume()
//        val fabRunningSession = requireView().findViewById<FloatingActionButton>(R.id.fab_running_session)
//        // set correct FAB depending if session is running.
//        // wait 100ms and check again to give Service time to stop after discarding session
//        runnable = object : Runnable {
//            override fun run() {
//                if (Musikus.serviceIsRunning) {
//                    // apparently there is a bug with hide() / View.GONE which causes the Toolbar to jump down
//                    // so use Invisible so that views don't get broken
//                    fabNewSessionView.visibility = View.INVISIBLE
//                    fabRunningSession.show()
//                } else {
//                    fabRunningSession.visibility = View.INVISIBLE
//                    fabNewSessionView.show()
//                }
//                if (Musikus.serviceIsRunning)
//                    handler.postDelayed(this, 500)
//            }
//        }
//        handler = Handler(Looper.getMainLooper()).also {
//            it.post(runnable)
//        }
//    }
//
//    // remove the callback. Otherwise, the runnable will keep going and when entering the activity again,
//    // there will be twice as much and so on...
//    override fun onStop() {
//        super.onStop()
//        handler.removeCallbacks(runnable)
//    }
//

