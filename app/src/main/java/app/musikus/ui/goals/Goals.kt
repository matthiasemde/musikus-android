/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package app.musikus.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.shared.ActionBar
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.MiniFABData
import app.musikus.shared.MultiFAB
import app.musikus.shared.MultiFabState
import app.musikus.shared.Selectable
import app.musikus.shared.SortMenu
import app.musikus.shared.ThemeMenu
import app.musikus.spacing
import app.musikus.ui.MainUIEvent
import app.musikus.ui.MainUiState
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.TimeProvider


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Goals(
    mainUiState: MainUiState,
    mainEventHandler: (event: MainUIEvent) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
    timeProvider: TimeProvider,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(
        enabled = uiState.actionModeUiState.isActionMode,
        onBack = viewModel::clearActionMode
    )

    BackHandler(
        enabled = mainUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = { mainEventHandler(MainUIEvent.CollapseMultiFab) }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = mainUiState.multiFabState,
                onStateChange = { state ->
                    mainEventHandler(MainUIEvent.ChangeMultiFabState(state))
                    if (state == MultiFabState.EXPANDED) {
                        viewModel.clearActionMode()
                    }
                },
                contentDescription = "Add",
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            viewModel.showDialog(oneShot = true)
                            mainEventHandler(MainUIEvent.CollapseMultiFab)
                        },
                        label = "One shot goal",
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            viewModel.showDialog(oneShot = false)
                            mainEventHandler(MainUIEvent.CollapseMultiFab)
                        },
                        label = "Regular goal",
                        icon = Icons.Rounded.Repeat,
                    )
                )
            )
        },
        topBar = {
            // TODO find a way to re-use Composable in every screen
            val mainMenuUiState = mainUiState.menuUiState
            val topBarUiState = uiState.topBarUiState
            LargeTopAppBar(
                title = { Text(text = topBarUiState.title) },
                scrollBehavior = scrollBehavior,
                actions = {
                    val sortMenuUiState = topBarUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = GoalsSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = "goals",
                        onShowMenuChanged = viewModel::onSortMenuShowChanged,
                        onSelectionHandler = viewModel::onSortModeSelected
                    )
                    IconButton(onClick = {
                        mainEventHandler(MainUIEvent.ShowMainMenu)
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu(
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
                            uniqueMenuItems = {}
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

            if (actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    uniqueActions = {
                        IconButton(onClick = {
                            viewModel.onArchiveAction()
                            mainEventHandler(MainUIEvent.ShowSnackbar(
                                message = "Archived",
                                onUndo = viewModel::onUndoArchiveAction
                            ))
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive",
                            )
                        }

                    },
                    editActionEnabled = { actionModeUiState.showEditAction },
                    onDismissHandler = viewModel::clearActionMode,
                    onEditHandler = viewModel::onEditAction,
                    onDeleteHandler = {
                        viewModel.onDeleteAction()
                        mainEventHandler(MainUIEvent.ShowSnackbar(
                            message = "Deleted",
                            onUndo = viewModel::onRestoreAction
                        ))
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = uiState.contentUiState

            // Goal List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.large,
                    end = MaterialTheme.spacing.large,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = contentUiState.goalsWithProgress,
                    key = { it.goal.instance.id },
                ) { (goal, progress) ->
                    Selectable(
                        modifier = Modifier.animateItemPlacement(),
                        selected = goal in contentUiState.selectedGoals,
                        onShortClick = { viewModel.onGoalClicked(goal, false) },
                        onLongClick = { viewModel.onGoalClicked(goal, true) }
                    ) {
                        GoalCard(
                            goal = goal,
                            progress = progress,
                            timeProvider = timeProvider
                        )
                    }
                }
            }

            /** Goal Dialog */
            val dialogUiState = uiState.dialogUiState

            if (dialogUiState != null) {
                if (dialogUiState.goalToEdit == null) {
                    GoalDialog(
                        dialogData = dialogUiState.dialogData,
                        libraryItems = dialogUiState.libraryItems,
                        onTargetChanged = viewModel::onTargetChanged,
                        onPeriodChanged = viewModel::onPeriodChanged,
                        onPeriodUnitChanged = viewModel::onPeriodUnitChanged,
                        onGoalTypeChanged = viewModel::onGoalTypeChanged,
                        onSelectedLibraryItemsChanged = viewModel::onLibraryItemsChanged,
                        onConfirmHandler = viewModel::onDialogConfirm,
                        onDismissHandler = viewModel::clearDialog,
                    )
                } else {
                    EditGoalDialog(
                        value = dialogUiState.dialogData.target,
                        onValueChanged = viewModel::onTargetChanged,
                        onConfirmHandler = viewModel::onDialogConfirm,
                        onDismissHandler = viewModel::clearDialog
                    )
                }
            }


            // Content Scrim for multiFAB

            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = mainUiState.multiFabState == MultiFabState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { mainEventHandler(MainUIEvent.CollapseMultiFab) }
                        )
                )
            }
        }
    )
}
