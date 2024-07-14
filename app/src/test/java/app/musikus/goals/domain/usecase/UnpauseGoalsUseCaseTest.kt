/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.UUIDConverter
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.data.FakeGoalRepository
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class UnpauseGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var unpauseGoals: UnpauseGoalsUseCase

    private lateinit var pauseGoals: PauseGoalsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        val cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(fakeGoalRepository, fakeTimeProvider)

        pauseGoals = PauseGoalsUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)

        unpauseGoals = UnpauseGoalsUseCase(fakeGoalRepository)

        runBlocking {
            fakeGoalRepository.addNewGoal(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = 1.hours,
                    startTimestamp = fakeTimeProvider.now(),
                ),
                libraryItemIds = emptyList()
            )
        }
    }

    @Test
    fun `Unpause paused goal, goal is unpaused`() = runTest {
        pauseGoals(listOf(UUIDConverter.fromInt(1)))

        val pausedGoal = fakeGoalRepository.allGoals.first().first()
        Truth.assertThat(pausedGoal.description.paused).isTrue()

        unpauseGoals(listOf(UUIDConverter.fromInt(1)))

        val unpausedGoal = fakeGoalRepository.allGoals.first().first()
        Truth.assertThat(unpausedGoal.description.paused).isFalse()
    }

    @Test
    fun `Unpause non existent goal, InvalidArgumentException`() = runTest {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(2)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Could not find goal(s) with descriptionId: [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun `Unpause archived goal, InvalidArgumentException`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(listOf(
            UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(archived = true)
        ))

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(1)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Cannot unpause archived goals: [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun `Unpause non-paused goal, InvalidArgumentException`() = runTest {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(1)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Cannot unpause goals that aren't paused: [00000000-0000-0000-0000-000000000001]"
        )
    }
}