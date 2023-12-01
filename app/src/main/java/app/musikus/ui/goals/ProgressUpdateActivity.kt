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

package app.musikus.ui.goals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.spacing
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

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
fun ProgressUpdate() {
    val progressUpdateState = rememberProgressUpdateState()

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
