/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.goals.data.FakeGoalRepository
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.library.data.FakeLibraryRepository
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

class UnarchiveGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase
    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase

    /** SUT */
    private lateinit var unarchiveGoalsUseCase: UnarchiveGoalsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(
            goalRepository = fakeGoalRepository,
            timeProvider = fakeTimeProvider,
        )

        archiveGoalsUseCase = ArchiveGoalsUseCase(
            goalRepository = fakeGoalRepository,
            cleanFutureGoalInstances = cleanFutureGoalInstancesUseCase,
        )

        /** SUT */
        unarchiveGoalsUseCase = UnarchiveGoalsUseCase(
            goalRepository = fakeGoalRepository,
            timeProvider = fakeTimeProvider,
        )

        updateGoalsUseCase = UpdateGoalsUseCase(
            goalRepository = fakeGoalRepository,
            archiveGoals = archiveGoalsUseCase,
            timeProvider = fakeTimeProvider,
        )

        runBlocking {
            fakeGoalRepository.addNewGoal(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
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
    fun `Unarchive non-existent goal description, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            archiveGoalsUseCase(listOf(UUIDConverter.fromInt(0)))
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal(s) with descriptionId(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun `Unarchive goal that is not archived, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            unarchiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))
        }

        assertThat(exception.message).isEqualTo(
            "Cannot unarchive goals that aren't archived: [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun `Unarchive paused goal during first instance, instance is not changed`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(
            listOf(
                UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(paused = true)
            )
        )

        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        unarchiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
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
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Unarchive paused goal in the future, new instance is created`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(
            listOf(
                UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(paused = true)
            )
        )

        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        fakeTimeProvider.advanceTimeBy(5.days)

        unarchiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
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
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Unarchive goal that has been finalized, new instance is created`() = runTest {
        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        fakeTimeProvider.advanceTimeBy(5.days)

        updateGoalsUseCase()

        unarchiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
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
                        modifiedAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(5.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Unarchive goal with active instance, goal is unarchived`() = runTest {
        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = true,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )

        unarchiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val unarchivedGoals = fakeGoalRepository.allGoals.first()

        assertThat(unarchivedGoals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
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
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }
}
