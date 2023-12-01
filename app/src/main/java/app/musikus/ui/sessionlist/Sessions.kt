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

package app.musikus.ui.sessionlist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.R
import app.musikus.datastore.ThemeSelections
import app.musikus.shared.ActionBar
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.Selectable
import app.musikus.shared.ThemeMenu
import app.musikus.spacing
import app.musikus.ui.activesession.ActiveSessionActivity
import app.musikus.utils.DateFormat
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.utils.musikusFormat
import app.musikus.viewmodel.MainViewModel
import app.musikus.viewmodel.SessionsViewModel
import java.time.ZonedDateTime
import java.util.UUID

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun Sessions(
    mainViewModel: MainViewModel,
    activity: AppCompatActivity?,
    sessionsViewModel: SessionsViewModel = viewModel(),
    editSession: (sessionId: UUID) -> Unit,
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val sessionsUiState by sessionsViewModel.uiState.collectAsStateWithLifecycle()

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
                    activity?.let {
                        val i = Intent(it, ActiveSessionActivity::class.java)
                        it.startActivity(i)
                        it.overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
                    }
                },
                expanded = fabExpanded,
            )
        },
        topBar = {
            val mainMenuUiState = mainUiState.menuUiState
            val topBarUiState = sessionsUiState.topBarUiState
            LargeTopAppBar(
                title = { Text(text = topBarUiState.title) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = mainViewModel::showMainMenu) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainMenuUiState.show,
                            onDismissHandler = mainViewModel::hideMainMenu,
                            onSelectionHandler = { commonSelection ->
                                mainViewModel.hideMainMenu()

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainViewModel.showThemeSubMenu()
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainViewModel.showExportImportDialog()
                                    }
                                }
                            },
                            uniqueMenuItems = { /* TODO UNIQUE Session MENU */ }
                        )
                        ThemeMenu(
                            expanded = mainMenuUiState.showThemeSubMenu,
                            currentTheme = mainViewModel.activeTheme.collectAsState(initial = ThemeSelections.DAY).value,
                            onDismissHandler = mainViewModel::hideThemeSubMenu,
                            onSelectionHandler = { theme ->
                                mainViewModel.hideThemeSubMenu()
                                mainViewModel.setTheme(theme)
                            }
                        )
                    }
                }
            )

            // Action bar
            val actionModeUiState = sessionsUiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = sessionsViewModel::clearActionMode,
                    onEditHandler = {
                        sessionsViewModel.onEditAction(editSession)
                    },
                    onDeleteHandler = {
                        sessionsViewModel.onDeleteAction()
                        mainViewModel.showSnackbar(
                            message = "Deleted ${actionModeUiState.numberOfSelections} sessions",
                            onUndo = sessionsViewModel::onRestoreAction
                        )
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = sessionsUiState.contentUiState

            // Session list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.large,
                    end = MaterialTheme.spacing.large,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 56.dp,
                ),
//                verticalArrangement = Arrangement.spacedBy(24.dp),
                state = sessionsListState,
            ) {
                contentUiState.sessionsForDaysForMonths.forEach { sessionsForDaysForMonth ->
                    item {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                        ) {
                            MonthHeader(
                                month = sessionsForDaysForMonth
                                    .sessionsForDays.first()
                                    .sessions.first()
                                    .startTimestamp.month.name,
                                onClickHandler = {
                                    sessionsViewModel.onMonthHeaderClicked(sessionsForDaysForMonth.specificMonth)
                                }
                            )
                        }
                    }
                    val monthVisible = sessionsForDaysForMonth.specificMonth in contentUiState.expandedMonths
                    sessionsForDaysForMonth.sessionsForDays.forEach { sessionsForDay ->
                        item {
                            AnimatedVisibility(
                                modifier = Modifier.animateItemPlacement(),
                                visible = monthVisible,
                                enter = scaleIn(),
                                exit = fadeOut()
                            ) {
                                DayHeader(
                                    timestamp = sessionsForDay.sessions.first().startTimestamp,
                                    totalPracticeDuration = sessionsForDay.totalPracticeDuration
                                )
                            }
                        }
                        items(
                            items = sessionsForDay.sessions,
                            key = { it.session.id }
                        ) { session ->
                            AnimatedVisibility(
                                modifier = Modifier.animateItemPlacement(),
                                visible = monthVisible,
                                enter = scaleIn(),
                                exit = fadeOut()
                            ) {
                                Selectable(
                                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                                    selected = session in contentUiState.selectedSessions,
                                    onShortClick = {
                                        sessionsViewModel.onSessionClicked(session)
                                    },
                                    onLongClick = {
                                        sessionsViewModel.onSessionClicked(session, longClick = true)
                                    },
                                ) {
                                    SessionCard(sessionWithSectionsWithLibraryItems = session)
                                }
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
    timestamp: ZonedDateTime,
    totalPracticeDuration: Int,
) {
    Row(
        modifier = Modifier
            .padding(vertical = MaterialTheme.spacing.small)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = timestamp.musikusFormat(
                listOf(DateFormat.WEEKDAY_ABBREVIATED, DateFormat.DAY_MONTH_YEAR)
            ),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = getDurationString(totalPracticeDuration, TIME_FORMAT_HUMAN_PRETTY).toString(),
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

