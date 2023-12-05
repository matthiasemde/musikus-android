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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.datastore.ThemeSelections
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
import app.musikus.utils.GoalsSortMode
import app.musikus.ui.MainViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Goals(
    mainViewModel: MainViewModel,
    goalsViewModel: GoalsViewModel = viewModel(),
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val goalsUiState by goalsViewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(
        enabled = goalsUiState.actionModeUiState.isActionMode,
        onBack = goalsViewModel::clearActionMode
    )

    BackHandler(
        enabled = mainUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = mainViewModel::collapseMultiFab
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = mainUiState.multiFabState,
                onStateChange = { state ->
                    mainViewModel.onMultiFabStateChanged(state)
                    if(state == MultiFabState.EXPANDED) {
                        goalsViewModel.clearActionMode()
                    }
                },
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            goalsViewModel.showDialog(oneShot = true)
                            mainViewModel.collapseMultiFab()
                        },
                        label = "One shot goal",
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            goalsViewModel.showDialog(oneShot = false)
                            mainViewModel.collapseMultiFab()
                        },
                        label = "Regular goal",
                        icon = Icons.Rounded.Repeat,
                    )
                ))
        },
        topBar = {
            val topBarUiState = goalsUiState.topBarUiState
            LargeTopAppBar(
                title = { Text( text = topBarUiState.title) },
                scrollBehavior = scrollBehavior,
                actions = {
                    val mainMenuUiState = mainUiState.menuUiState
                    val sortMenuUiState = topBarUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = GoalsSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        onShowMenuChanged = goalsViewModel::onSortMenuShowChanged,
                        onSelectionHandler = goalsViewModel::onSortModeSelected
                    )
                    IconButton(onClick = {
                        mainViewModel.showMainMenu()
                    }) {
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
                            uniqueMenuItems = {
                                val overflowMenuUiState = topBarUiState.overflowMenuUiState
                                DropdownMenuItem(
                                    text = { Text(text = "Show paused goals") },
                                    trailingIcon = {
                                        Switch(
                                            checked = overflowMenuUiState.showPausedGoals,
                                            onCheckedChange = goalsViewModel::onPausedGoalsChanged,
                                        )
                                    },
                                    onClick = {
                                        goalsViewModel.onPausedGoalsChanged(!overflowMenuUiState.showPausedGoals)
                                    },
                                )
                            }
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
            val actionModeUiState = goalsUiState.actionModeUiState

            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    uniqueActions = {
                        if(actionModeUiState.showPauseAction) {
                            IconButton(onClick = goalsViewModel::onPauseAction) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause",
                                )
                            }
                        }
                        if(actionModeUiState.showUnpauseAction) {
                            IconButton(onClick = goalsViewModel::onUnpauseAction) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Unpause",
                                )
                            }
                        }
                        IconButton(onClick = {
                            goalsViewModel.onArchiveAction()
                            mainViewModel.showSnackbar(
                                message = "Archived",
                                onUndo = goalsViewModel::onUndoArchiveAction
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive",
                            )
                        }

                    },
                    editActionEnabled = { actionModeUiState.showEditAction },
                    onDismissHandler = goalsViewModel::clearActionMode,
                    onEditHandler = goalsViewModel::onEditAction,
                    onDeleteHandler = {
                        goalsViewModel.onDeleteAction()
                        mainViewModel.showSnackbar(
                            message = "Deleted",
                            onUndo = goalsViewModel::onRestoreAction
                        )
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = goalsUiState.contentUiState

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
                    items= contentUiState.goalsWithProgress,
                    key = { it.goal.instance.id },
                ) { (goal, progress) ->
                    Selectable(
                        modifier = Modifier.animateItemPlacement(),
                        selected = goal in contentUiState.selectedGoals,
                        onShortClick = { goalsViewModel.onGoalClicked(goal, false) },
                        onLongClick = { goalsViewModel.onGoalClicked(goal, true) }
                    ) {
                        GoalCard(goal = goal, progress = progress)
                    }
                }
            }

            /** Goal Dialog */
            val dialogUiState = goalsUiState.dialogUiState

            if(dialogUiState != null) {
                if (dialogUiState.goalToEdit == null) {
                    GoalDialog(
                        dialogData = dialogUiState.dialogData,
                        libraryItems = dialogUiState.libraryItems,
                        onTargetChanged = goalsViewModel::onTargetChanged,
                        onPeriodChanged = goalsViewModel::onPeriodChanged,
                        onPeriodUnitChanged = goalsViewModel::onPeriodUnitChanged,
                        onGoalTypeChanged = goalsViewModel::onGoalTypeChanged,
                        onSelectedLibraryItemsChanged = goalsViewModel::onLibraryItemsChanged,
                        onConfirmHandler = goalsViewModel::onDialogConfirm,
                        onDismissHandler = goalsViewModel::clearDialog,
                    )
                } else {
                    EditGoalDialog(
                        value = dialogUiState.dialogData.target,
                        onValueChanged = goalsViewModel::onTargetChanged,
                        onConfirmHandler = goalsViewModel::onDialogConfirm,
                        onDismissHandler = goalsViewModel::clearDialog
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
                            onClick = mainViewModel::collapseMultiFab
                        )
                )
            }
        }
    )
}
