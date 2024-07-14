/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.settings.data.UserPreferencesRepository
import app.musikus.settings.domain.usecase.GetGoalSortInfoUseCase
import app.musikus.utils.FakeTimeProvider
import app.musikus.core.domain.GoalsSortMode
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.goals.domain.usecase.SortGoalsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SortGoalsUseCaseTest {
    private lateinit var fakeUserPreferencesRepository: UserPreferencesRepository

    /** SUT */
    private lateinit var sortGoals: SortGoalsUseCase

    private val baseDescription = GoalDescription(
        id = UUIDConverter.fromInt(1),
        createdAt = FakeTimeProvider.START_TIME,
        modifiedAt = FakeTimeProvider.START_TIME,
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.WEEK,
        progressType = GoalProgressType.TIME,
        paused = false,
        archived = false,
        customOrder = null
    )

    private val baseInstance = GoalInstance(
        id = UUIDConverter.fromInt(2),
        createdAt = FakeTimeProvider.START_TIME,
        modifiedAt = FakeTimeProvider.START_TIME,
        descriptionId = UUIDConverter.fromInt(1),
        previousInstanceId = null,
        startTimestamp = FakeTimeProvider.START_TIME,
        targetSeconds = 3,
        endTimestamp = null
    )

    private val goals = listOf(
        GoalDescriptionWithInstancesAndLibraryItems(
            description = baseDescription,
            instances = listOf(baseInstance),
            libraryItems = emptyList()
        ),
        GoalDescriptionWithInstancesAndLibraryItems(
            description = baseDescription.copy(
                id = UUIDConverter.fromInt(3),
                createdAt = baseDescription.createdAt.plus(1.seconds.toJavaDuration()),
                modifiedAt = baseDescription.modifiedAt.plus(1.seconds.toJavaDuration()),
                periodUnit = GoalPeriodUnit.DAY,
            ),
            instances = listOf(baseInstance.copy(
                id = UUIDConverter.fromInt(4),
                descriptionId = UUIDConverter.fromInt(3),
                targetSeconds = 1,
            )),
            libraryItems = emptyList()
        ),
        GoalDescriptionWithInstancesAndLibraryItems(
            description = baseDescription.copy(
                id = UUIDConverter.fromInt(5),
                createdAt = baseDescription.createdAt.plus(2.seconds.toJavaDuration()),
                modifiedAt = baseDescription.modifiedAt.plus(2.seconds.toJavaDuration()),
                periodUnit = GoalPeriodUnit.MONTH,
            ),
            instances = listOf(baseInstance.copy(
                id = UUIDConverter.fromInt(6),
                descriptionId = UUIDConverter.fromInt(5),
                targetSeconds = 5,
            )),
            libraryItems = emptyList()
        ),
        GoalDescriptionWithInstancesAndLibraryItems(
            description = baseDescription.copy(
                id = UUIDConverter.fromInt(7),
                createdAt = baseDescription.createdAt.plus(3.seconds.toJavaDuration()),
                modifiedAt = baseDescription.modifiedAt.plus(3.seconds.toJavaDuration()),
                periodUnit = GoalPeriodUnit.DAY,
                periodInPeriodUnits = 9,
            ),
            instances = listOf(baseInstance.copy(
                id = UUIDConverter.fromInt(8),
                descriptionId = UUIDConverter.fromInt(7),
                targetSeconds = 4,
            )),
            libraryItems = emptyList()
        ),
        GoalDescriptionWithInstancesAndLibraryItems(
            description = baseDescription.copy(
                id = UUIDConverter.fromInt(9),
                createdAt = baseDescription.createdAt.plus(4.seconds.toJavaDuration()),
                modifiedAt = baseDescription.modifiedAt.plus(4.seconds.toJavaDuration()),
                periodUnit = GoalPeriodUnit.WEEK,
                periodInPeriodUnits = 6,
            ),
            instances = listOf(baseInstance.copy(
                id = UUIDConverter.fromInt(10),
                descriptionId = UUIDConverter.fromInt(9),
                targetSeconds = 2,
            )),
            libraryItems = emptyList()
        ),
    )

    @BeforeEach
    fun setUp() {
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()

        /** SUT */
        sortGoals = SortGoalsUseCase(
            GetGoalSortInfoUseCase(fakeUserPreferencesRepository)
        )
    }

    @Test
    fun `Sort goals, goals are sorted by 'date added' descending`() = runTest {
        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(9, 7, 5, 3, 1).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted by default sort mode
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }

    @Test
    fun `Set goal sort mode to 'date added' ascending, goals are sorted correctly`() = runTest {
        // Set sort mode to 'date added' ascending
        fakeUserPreferencesRepository.updateGoalSortInfo(
            SortInfo(
                mode = GoalsSortMode.DATE_ADDED,
                direction = SortDirection.ASCENDING,
            )
        )

        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(1, 3, 5, 7, 9).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted correctly
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }

    @Test
    fun `Set goal sort mode to 'target' descending, goals are sorted correctly`() = runTest {
        // Set sort mode to 'date modified' descending
        fakeUserPreferencesRepository.updateGoalSortInfo(
            SortInfo(
                mode = GoalsSortMode.TARGET,
                direction = SortDirection.DESCENDING,
            )
        )

        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(
            5,  // target: 5 seconds
            7,  // target: 4 seconds
            1,  // target: 3 seconds
            9,  // target: 2 seconds
            3   // target: 1 seconds
        ).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted correctly
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }

    @Test
    fun `Set goal sort mode to 'target' ascending, goals are sorted correctly`() = runTest {
        // Set sort mode to 'date modified' ascending
        fakeUserPreferencesRepository.updateGoalSortInfo(
            SortInfo(
                mode = GoalsSortMode.TARGET,
                direction = SortDirection.ASCENDING,
            )
        )

        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(
            3,  // target: 1 seconds
            9,  // target: 2 seconds
            1,  // target: 3 seconds
            7,  // target: 4 seconds
            5   // target: 5 seconds
        ).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted correctly
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }

    @Test
    fun `Set goal sort mode to 'period' descending, goals are sorted correctly`() = runTest {
        // Set sort mode to 'period' descending
        fakeUserPreferencesRepository.updateGoalSortInfo(
            SortInfo(
                mode = GoalsSortMode.PERIOD,
                direction = SortDirection.DESCENDING,
            )
        )

        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(
            5,  // period: 1 month
            9,  // period: 6 weeks
            1,  // period: 1 week
            7,  // period: 9 days
            3,  // period: 1 day
        ).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted correctly
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }

    @Test
    fun `Set goal sort mode to 'period' ascending, goals are sorted correctly`() = runTest {
        // Set sort mode to 'period' ascending
        fakeUserPreferencesRepository.updateGoalSortInfo(
            SortInfo(
                mode = GoalsSortMode.PERIOD,
                direction = SortDirection.ASCENDING,
            )
        )

        val sortedGoals = sortGoals(goalsFlow = flowOf(goals)).first()

        val expectedDescriptionIds = listOf(
            3,  // period: 1 day
            7,  // period: 9 days
            1,  // period: 1 week
            9,  // period: 6 weeks
            5,  // period: 1 month
        ).map { UUIDConverter.fromInt(it) }

        // Assert that the goals are sorted correctly
        assertThat(sortedGoals.map { it.description.id }).isEqualTo(expectedDescriptionIds)
    }
}