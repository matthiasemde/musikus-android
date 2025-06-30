/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainUiEventHandler
import app.musikus.core.presentation.MainUiState
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.components.ActionBar
import app.musikus.core.presentation.components.DeleteConfirmationBottomSheet
import app.musikus.core.presentation.components.MiniFABData
import app.musikus.core.presentation.components.MultiFAB
import app.musikus.core.presentation.components.MultiFabState
import app.musikus.core.presentation.components.Selectable
import app.musikus.core.presentation.components.SortMenu
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import app.musikus.goals.data.GoalsSortMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    viewModel: GoalsViewModel = hiltViewModel(),
    navigateUp: () -> Unit,
    timeProvider: TimeProvider,
    bottomBarHeight: Dp,
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
        contentWindowInsets = WindowInsets(bottom = bottomBarHeight), // makes sure FAB is above the bottom Bar
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            MultiFAB(
                state = mainUiState.multiFabState,
                onStateChange = { newState ->
                    if (newState == MultiFabState.EXPANDED) {
                        mainEventHandler(MainUiEvent.ExpandMultiFab)
                        eventHandler(GoalsUiEvent.ClearActionMode)
                    } else {
                        mainEventHandler(MainUiEvent.CollapseMultiFab)
                    }
                },
                contentDescription = stringResource(id = R.string.goals_screen_multi_fab_description),
                miniFABs = listOf(
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = true))
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = stringResource(id = R.string.goals_non_repeating),
                        icon = Icons.Filled.LocalFireDepartment,
                    ),
                    MiniFABData(
                        onClick = {
                            eventHandler(GoalsUiEvent.AddGoalButtonPressed(oneShot = false))
                            mainEventHandler(MainUiEvent.CollapseMultiFab)
                        },
                        label = stringResource(id = R.string.goals_repeating),
                        icon = Icons.Rounded.Repeat,
                    )
                )
            )
        },
        topBar = {
            val topBarUiState = uiState.topBarUiState

            MusikusTopBar(
                isTopLevel = true,
                title = UiText.StringResource(R.string.goals_title),
                scrollBehavior = scrollBehavior,
                openMainMenu = { mainEventHandler(MainUiEvent.OpenMainMenu) },
                actions = {
                    val sortMenuUiState = topBarUiState.sortMenuUiState
                    SortMenu(
                        sortModes = GoalsSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = stringResource(
                            id = R.string.goals_screen_top_bar_sort_menu_item_description
                        ),
                        onSelectionHandler = { eventHandler(GoalsUiEvent.GoalSortModeSelected(it)) }
                    )
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
                                contentDescription = stringResource(
                                    id = R.string.components_action_bar_archive_button_description
                                ),
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

            val verticalArrangementSpacing = MaterialTheme.spacing.medium

            // Goal List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.large,
                    end = MaterialTheme.spacing.large,
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(verticalArrangementSpacing),
            ) {
                // header and footer spacers replace contentPadding
                // but also serve to anchor the column when inserting items
                item {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }

                items(
                    items = contentUiState.currentGoals,
                    key = { it.instance.id },
                ) { goal ->
                    val descriptionId = goal.description.description.id
                    Selectable(
                        modifier = Modifier.animateItem(),
                        selected = descriptionId in contentUiState.selectedGoalIds,
                        onShortClick = {
                            eventHandler(
                                GoalsUiEvent.GoalPressed(
                                    goal,
                                    longClick = false
                                )
                            )
                        },
                        onLongClick = {
                            eventHandler(
                                GoalsUiEvent.GoalPressed(
                                    goal,
                                    longClick = true
                                )
                            )
                        }
                    ) {
                        GoalCard(
                            goal = goal,
                            timeProvider = timeProvider
                        )
                    }
                }

                // extra large spacer as footer for clearing the fab button
                item {
                    Spacer(modifier = Modifier.height(56.dp - verticalArrangementSpacing))
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
                        text = stringResource(id = R.string.goals_screen_hint),
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
                val snackbarMessage = stringResource(
                    id = if (deleteOrArchiveDialogUiState.isArchiveAction) {
                        R.string.goals_screen_snackbar_archived
                    } else {
                        R.string.goals_screen_snackbar_deleted
                    }
                )

                DeleteConfirmationBottomSheet(
                    explanation = UiText.PluralResource(
                        resId =
                        if (deleteOrArchiveDialogUiState.isArchiveAction) {
                            R.plurals.goals_screen_archive_goal_dialog_explanation
                        } else {
                            R.plurals.goals_screen_delete_goal_dialog_explanation
                        },
                        quantity = deleteOrArchiveDialogUiState.numberOfSelections,
                        deleteOrArchiveDialogUiState.numberOfSelections
                    ),
                    confirmationIcon = UiIcon.DynamicIcon(
                        if (deleteOrArchiveDialogUiState.isArchiveAction) {
                            Icons.Rounded.Archive
                        } else {
                            Icons.Rounded.Delete
                        }
                    ),
                    confirmationText = UiText.StringResource(
                        resId = if (deleteOrArchiveDialogUiState.isArchiveAction) {
                            R.string.goals_screen_archive_goal_dialog_confirm
                        } else {
                            R.string.goals_screen_delete_goal_dialog_confirm
                        },
                        deleteOrArchiveDialogUiState.numberOfSelections
                    ),
                    onDismiss = { eventHandler(GoalsUiEvent.DeleteOrArchiveDialogDismissed) },
                    onConfirm = {
                        eventHandler(GoalsUiEvent.DeleteOrArchiveDialogConfirmed)
                        mainEventHandler(
                            MainUiEvent.ShowSnackbar(
                                message = snackbarMessage,
                                onUndo = { eventHandler(GoalsUiEvent.UndoButtonPressed) }
                            )
                        )
                    }
                )
            }

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier.zIndex(1f),
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
