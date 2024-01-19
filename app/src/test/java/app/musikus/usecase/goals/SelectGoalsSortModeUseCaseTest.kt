/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class SelectGoalsSortModeUseCaseTest {
    private lateinit var selectGoalSortMode: SelectGoalsSortModeUseCase
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        selectGoalSortMode = SelectGoalsSortModeUseCase(
            userPreferencesRepository = fakeUserPreferencesRepository,
        )
    }

    @Test
    fun `Select new sort mode, sort mode is updated`() = runTest {
        // Set initial sort mode
        fakeUserPreferencesRepository.updateGoalSortInfo(
            sortInfo = SortInfo(
                mode = GoalsSortMode.DATE_ADDED,
                direction = SortDirection.DEFAULT
            ),
        )

        // Select a new sort mode
        selectGoalSortMode(
            sortMode = GoalsSortMode.TARGET,
        )

        // Assert that the sort mode was updated with Default sort direction
        val sortInfo = fakeUserPreferencesRepository.goalSortInfo.first()
        assertThat(sortInfo)
            .isEqualTo(SortInfo(
                mode = GoalsSortMode.TARGET,
                direction = SortDirection.DEFAULT,
            ))
    }

    @Test
    fun `Select current sort mode, sort direction is inverted`() = runTest {
        // Set initial sort mode
        fakeUserPreferencesRepository.updateGoalSortInfo(
            sortInfo = SortInfo(
                mode = GoalsSortMode.DATE_ADDED,
                direction = SortDirection.DESCENDING
            ),
        )

        // Select the current sort mode
        selectGoalSortMode(
            sortMode = GoalsSortMode.DATE_ADDED,
        )

        // Assert that the sort mode was updated with inverted sort direction
        val sortInfo = fakeUserPreferencesRepository.goalSortInfo.first()
        assertThat(sortInfo)
            .isEqualTo(SortInfo(
                mode = GoalsSortMode.DATE_ADDED,
                direction = SortDirection.ASCENDING,
            ))
    }
}