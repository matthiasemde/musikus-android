/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
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
import app.musikus.ui.components.ActionBar
import app.musikus.ui.components.CommonMenuSelections
import app.musikus.ui.components.MainMenu
import app.musikus.ui.components.MiniFABData
import app.musikus.ui.components.MultiFAB
import app.musikus.ui.components.MultiFabState
import app.musikus.ui.components.Selectable
import app.musikus.ui.components.SortMenu
import app.musikus.ui.components.ThemeMenu
import app.musikus.ui.MainUiEvent
import app.musikus.ui.MainUiEventHandler
import app.musikus.ui.MainUiState
import app.musikus.ui.Screen
import app.musikus.ui.theme.spacing
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.TimeProvider


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Goals(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    navigateTo: (Screen) -> Unit,
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
        onBack = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = mainUiState.multiFabState,
                onStateChange = { state ->
                    mainEventHandler(MainUiEvent.ChangeMultiFabState(state))
                    if (state == MultiFabState.EXPANDED) {
                        viewModel.clearActionMode()
                    }
                },
                contentDescription = "Add",
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            viewModel.showDialog(oneShot = true)
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = "One shot goal",
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            viewModel.showDialog(oneShot = false)
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
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
                        mainEventHandler(MainUiEvent.ShowMainMenu)
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu(
                            show = mainMenuUiState.show,
                            onDismissHandler = { mainEventHandler(MainUiEvent.HideMainMenu) },
                            onSelectionHandler = { commonSelection ->
                                mainEventHandler(MainUiEvent.HideMainMenu)

                                when (commonSelection) {
                                    CommonMenuSelections.SETTINGS -> { navigateTo(Screen.Settings)}
                                }
                            },
                            uniqueMenuItems = {}
                        )
                        ThemeMenu(
                            expanded = mainMenuUiState.showThemeSubMenu,
                            currentTheme = mainUiState.activeTheme,
                            onDismissHandler = { mainEventHandler(MainUiEvent.HideThemeSubMenu) },
                            onSelectionHandler = { theme ->
                                mainEventHandler(MainUiEvent.HideThemeSubMenu)
                                mainEventHandler(MainUiEvent.SetTheme(theme))
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
                            mainEventHandler(MainUiEvent.ShowSnackbar(
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
                        mainEventHandler(MainUiEvent.ShowSnackbar(
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
                    items = contentUiState.currentGoals,
                    key = { it.instance.id },
                ) { goal ->
                    val descriptionId = goal.description.description.id
                    Selectable(
                        modifier = Modifier.animateItemPlacement(),
                        selected = descriptionId in contentUiState.selectedGoalIds,
                        onShortClick = { viewModel.onGoalClicked(goal, false) },
                        onLongClick = { viewModel.onGoalClicked(goal, true) }
                    ) {
                        GoalCard(
                            goal = goal,
                            timeProvider = timeProvider
                        )
                    }
                }
            }

            /** Goal Dialog */
            val dialogUiState = uiState.dialogUiState

            if (dialogUiState != null) {
                if (dialogUiState.goalToEditId == null) {
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
                            onClick = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
                        )
                )
            }
        }
    )
}
