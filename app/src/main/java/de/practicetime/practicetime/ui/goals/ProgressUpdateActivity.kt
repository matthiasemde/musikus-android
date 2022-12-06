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
 */

package de.practicetime.practicetime.ui.goals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.spacing
import de.practicetime.practicetime.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import java.util.*

const val PROGRESS_UPDATED = 1337

class ProgressUpdateState @OptIn(ExperimentalMaterial3Api::class) constructor(
    private val coroutineScope: CoroutineScope,
    val scrollBehavior: TopAppBarScrollBehavior
) {
    val updatedGoalIds = mutableStateListOf<UUID>()
    val updatedGoalOffsets = mutableStateListOf<Int>()

    // animation
    val isAnimating = mutableStateOf(true)

    fun skipAnimation() {
        isAnimating.value = false
        // TODO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberProgressUpdateState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
) = remember(coroutineScope) { ProgressUpdateState(coroutineScope, scrollBehavior) }


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgressUpdate(
    mainViewModel: MainViewModel,
) {
    val progressUpdateState = rememberProgressUpdateState()

//    LaunchedEffect(key1 = true) {
//        val latestSessionId = 20L
//        Log.d("PROGRESS", latestSessionId.toString())
//        val latestSession = PracticeTime.sessionDao.getWithSectionsWithLibraryItemsWithGoals(latestSessionId)
//        val goalProgress = PracticeTime.goalDescriptionDao.computeGoalProgressForSession(latestSession)
//
//        Log.d("PROGRESS", "Start animation")
//        goalProgress.entries.forEach { (goalId, progress) ->
//            delay(1000)
//            progressUpdateState.updatedGoalIds.add(goalId)
//            delay(1000)
//            progressUpdateState.updatedGoalOffsets.add(progress)
//        }
//    }

    Scaffold(
        modifier = Modifier.nestedScroll(progressUpdateState.scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.progressUpdateTitle)) },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (progressUpdateState.isAnimating.value) {
                        TextButton(
                            onClick = { progressUpdateState.skipAnimation() },
                            content = { Text(stringResource(id = R.string.progressUpdateSkip)) }
                        )
                    } else {
                        TextButton(
                            onClick = {},
//                            onClick = { mainViewModel.navigateTo(Screen.Sessions.route) },
                            content = { Text(stringResource(id = R.string.progressUpdateContinue)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        val goals = emptyList<GoalInstanceWithDescriptionWithLibraryItems>()

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 56.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = goals.filter {
                    it.description.description.id in progressUpdateState.updatedGoalIds
                }.reversed(),
                key = { it.description.description.id }
            ) { goal ->
                val index = progressUpdateState.updatedGoalIds.indexOf(goal.description.description.id)
                GoalCard(
                    modifier = Modifier.animateItemPlacement(),
                    goal = goal,
                    progressOffset = if (progressUpdateState.updatedGoalOffsets.size > index)
                        progressUpdateState.updatedGoalOffsets[index] else 0,
                )
            }
        }
    }
}
