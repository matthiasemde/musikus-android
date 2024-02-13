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
import androidx.compose.material.icons.rounded.Delete
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
import app.musikus.ui.MainUiEvent
import app.musikus.ui.MainUiEventHandler
import app.musikus.ui.MainUiState
import app.musikus.ui.Screen
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.theme.spacing
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.TimeProvider
import app.musikus.utils.UiIcon
import app.musikus.utils.UiText


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    navigateTo: (Screen) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
    timeProvider: TimeProvider,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler: GoalsUiEventHandler = viewModel::onUiEvent

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(
        enabled = uiState.actionModeUiState.isActionMode,
        onBack = { eventHandler(GoalsUiEvent.ClearActionMode) }
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
                        eventHandler(GoalsUiEvent.ClearActionMode)
                    }
                },
                contentDescription = "Add",
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = true))
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = "One shot goal",
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = false))
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
                        onShowMenuChanged = { eventHandler(GoalsUiEvent.GoalSortMenuPressed) },
                        onSelectionHandler = { eventHandler(GoalsUiEvent.GoalSortModeSelected(it)) }
                    )
                    Box {
                        IconButton(onClick = {
                            mainEventHandler(MainUiEvent.ShowMainMenu)
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "more")
                        }
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
                    }
                }

            )

            // Action bar
            val actionModeUiState = uiState.actionModeUiState

            if (actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    uniqueActions = {
                        IconButton(onClick = { eventHandler(GoalsUiEvent.ArchiveButtonPressed) }) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive",
                            )
                        }
                    },
                    editActionEnabled = { actionModeUiState.showEditAction },
                    onDismissHandler = { eventHandler(GoalsUiEvent.ClearActionMode) },
                    onEditHandler = { eventHandler(GoalsUiEvent.EditButtonPressed) },
                    onDeleteHandler = { eventHandler(GoalsUiEvent.DeleteButtonPressed) }
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
                        onShortClick = { eventHandler(GoalsUiEvent.GoalPressed(goal, longClick = false)) },
                        onLongClick = { eventHandler(GoalsUiEvent.GoalPressed(goal, longClick = true)) }
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

            val addOrEditDialogUiState = dialogUiState.addOrEditDialogUiState

            if (addOrEditDialogUiState != null) {
                GoalDialog(
                    uiState = addOrEditDialogUiState,
                    eventHandler = { eventHandler(GoalsUiEvent.DialogUiEvent(it)) },
                )
            }


            // Delete Goal Dialog

            val deleteOrArchiveDialogUiState = dialogUiState.deleteOrArchiveDialogUiState

            if (deleteOrArchiveDialogUiState != null) {
                DeleteConfirmationBottomSheet(
                    explanation = UiText.DynamicString(
                        if(deleteOrArchiveDialogUiState.isArchiveAction) {
                            "Archive goals? They will remain in your statistics but progress towards them will no longer be tracked."
                        } else {
                            "Delete goals? They will be erased from your statistics and cannot be restored. If you want to keep your statistics, consider archiving them instead."
                        }
                    ),
                    confirmationIcon = UiIcon.DynamicIcon(if(deleteOrArchiveDialogUiState.isArchiveAction) Icons.Rounded.Archive else Icons.Rounded.Delete),
                    confirmationText = UiText.DynamicString("${if(deleteOrArchiveDialogUiState.isArchiveAction) "Archive" else "Delete"} forever (${deleteOrArchiveDialogUiState.numberOfSelections})"),
                    onDismiss = { eventHandler(GoalsUiEvent.DeleteOrArchiveDialogDismissed) },
                    onConfirm = {
                        eventHandler(GoalsUiEvent.DeleteOrArchiveDialogConfirmed)
                        mainEventHandler(MainUiEvent.ShowSnackbar(
                            message = if (deleteOrArchiveDialogUiState.isArchiveAction) "Archived" else "Deleted",
                            onUndo = { eventHandler(GoalsUiEvent.UndoButtonPressed) }
                        ))
                    }
                )
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
