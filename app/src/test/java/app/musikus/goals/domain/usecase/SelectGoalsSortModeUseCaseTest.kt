/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.FakeUserPreferencesRepository
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.goals.data.GoalsSortMode
import com.google.common.truth.Truth
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
                direction = SortDirection.Companion.DEFAULT
            ),
        )

        // Select a new sort mode
        selectGoalSortMode(
            sortMode = GoalsSortMode.TARGET,
        )

        // Assert that the sort mode was updated with Default sort direction
        val sortInfo = fakeUserPreferencesRepository.goalSortInfo.first()
        Truth.assertThat(sortInfo)
            .isEqualTo(
                SortInfo(
                    mode = GoalsSortMode.TARGET,
                    direction = SortDirection.Companion.DEFAULT,
                )
            )
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
        Truth.assertThat(sortInfo)
            .isEqualTo(
                SortInfo(
                    mode = GoalsSortMode.DATE_ADDED,
                    direction = SortDirection.ASCENDING,
                )
            )
    }
}
