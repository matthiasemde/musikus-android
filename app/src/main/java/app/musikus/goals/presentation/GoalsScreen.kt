/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.presentation

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.HomeUiEvent
import app.musikus.core.presentation.HomeUiEventHandler
import app.musikus.core.presentation.HomeUiState
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.Screen
import app.musikus.ui.components.ActionBar
import app.musikus.ui.components.CommonMenuSelections
import app.musikus.ui.components.DeleteConfirmationBottomSheet
import app.musikus.ui.components.MainMenu
import app.musikus.ui.components.MiniFABData
import app.musikus.ui.components.MultiFAB
import app.musikus.ui.components.MultiFabState
import app.musikus.ui.components.Selectable
import app.musikus.ui.components.SortMenu
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.domain.GoalsSortMode
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    mainEventHandler: MainUiEventHandler,
    homeUiState: HomeUiState,
    homeEventHandler: HomeUiEventHandler,
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
        enabled = homeUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = { homeEventHandler(HomeUiEvent.CollapseMultiFab) }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = homeUiState.multiFabState,
                onStateChange = { newState ->
                    if (newState == MultiFabState.EXPANDED) {
                        homeEventHandler(HomeUiEvent.ExpandMultiFab)
                        eventHandler(GoalsUiEvent.ClearActionMode)
                    } else {
                        homeEventHandler(HomeUiEvent.CollapseMultiFab)
                    }
                },
                contentDescription = "Add",
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = true))
                            homeEventHandler(HomeUiEvent.CollapseMultiFab)
                        },
                        label = "One shot goal",
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = false))
                            homeEventHandler(HomeUiEvent.CollapseMultiFab)
                        },
                        label = "Regular goal",
                        icon = Icons.Rounded.Repeat,
                    )
                )
            )
        },
        topBar = {
            // TODO find a way to re-use Composable in every screen
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
                            homeEventHandler(HomeUiEvent.ShowMainMenu)
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "more")
                        }
                        MainMenu(
                            show = homeUiState.showMainMenu,
                            onDismiss = { homeEventHandler(HomeUiEvent.HideMainMenu) },
                            onSelection = { commonSelection ->
                                homeEventHandler(HomeUiEvent.HideMainMenu)

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
                        onShortClick = { eventHandler(
                            GoalsUiEvent.GoalPressed(
                                goal,
                                longClick = false
                            )
                        ) },
                        onLongClick = { eventHandler(
                            GoalsUiEvent.GoalPressed(
                                goal,
                                longClick = true
                            )
                        ) }
                    ) {
                        GoalCard(
                            goal = goal,
                            timeProvider = timeProvider
                        )
                    }
                }
            }

            // Show hint if there are no goals
            if (contentUiState.showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.goalsFragmentHint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }


            /**
             *  ---------------------- Dialogs ----------------------
             */

            val dialogUiState = uiState.dialogUiState

            // Goal Dialog
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
                        mainEventHandler(
                            MainUiEvent.ShowSnackbar(
                            message = if (deleteOrArchiveDialogUiState.isArchiveAction) "Archived" else "Deleted",
                            onUndo = { eventHandler(GoalsUiEvent.UndoButtonPressed) }
                        ))
                    }
                )
            }

            // Content Scrim for multiFAB

            AnimatedVisibility(
                modifier = Modifier.zIndex(1f),
                visible = homeUiState.multiFabState == MultiFabState.EXPANDED,
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
                            onClick = { homeEventHandler(HomeUiEvent.CollapseMultiFab) }
                        )
                )
            }
        }
    )
}