/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class PauseGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var addGoalUseCase: AddGoalUseCase
    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase
    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase

    /** SUT */
    private lateinit var pauseGoals: PauseGoalsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        addGoalUseCase = AddGoalUseCase(fakeGoalRepository, fakeLibraryRepository, fakeTimeProvider)
        archiveGoalsUseCase = ArchiveGoalsUseCase(fakeGoalRepository)
        updateGoalsUseCase = UpdateGoalsUseCase(fakeGoalRepository, archiveGoalsUseCase, fakeTimeProvider)
        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(fakeGoalRepository, fakeTimeProvider)

        /** SUT */
        pauseGoals = PauseGoalsUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)

        runBlocking {
            addGoalUseCase(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = 1.hours,
                ),
                libraryItemIds = emptyList()
            )
        }
    }

    @Test
    fun `Pause goal, goal is paused`() = runTest {
        var goals = fakeGoalRepository.allGoals.first()
        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )

        fakeTimeProvider.advanceTimeBy(1.days)
        pauseGoals(listOf(UUIDConverter.fromInt(1)))

        goals = fakeGoalRepository.allGoals.first()
        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = true,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun pauseGoalWithFutureInstances_goalIsPausedAndInstancesAreCleaned() = runTest {
        fakeTimeProvider.advanceTimeBy(4.days)

        updateGoalsUseCase()

        fakeTimeProvider.revertTimeBy(3.days)

        pauseGoals(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = true,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME.plus(4.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(1.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME.plus(4.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(1.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Pause non existent goal, InvalidArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            pauseGoals(listOf(UUIDConverter.fromInt(2)))
        }
        
        assertThat(exception.message).isEqualTo(
            "Could not find goal(s) with descriptionId: [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun `Pause archived goal, InvalidArgumentException`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(listOf(
            UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(archived = true)
        ))

        val exception = assertThrows<IllegalArgumentException> {
            pauseGoals(listOf(UUIDConverter.fromInt(1)))
        }

        assertThat(exception.message).isEqualTo(
            "Cannot pause archived goals: [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun `Pause already paused goal, InvalidArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            repeat(2) {
                pauseGoals(listOf(UUIDConverter.fromInt(1)))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot pause already paused goals: [00000000-0000-0000-0000-000000000001]"
        )
    }
}