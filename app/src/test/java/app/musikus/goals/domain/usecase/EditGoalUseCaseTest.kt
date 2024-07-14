/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.data.entities.InvalidGoalDescriptionException
import app.musikus.goals.data.entities.InvalidGoalInstanceException
import app.musikus.goals.data.FakeGoalRepository
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
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

class EditGoalUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var addGoal: AddGoalUseCase
    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase
    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase

    /** SUT */
    private lateinit var editGoalUseCase: EditGoalUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(fakeGoalRepository, fakeTimeProvider)
        addGoal = AddGoalUseCase(
            fakeGoalRepository,
            GetAllLibraryItemsUseCase(fakeLibraryRepository),
            fakeTimeProvider
        )
        archiveGoalsUseCase = ArchiveGoalsUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)
        updateGoalsUseCase = UpdateGoalsUseCase(fakeGoalRepository, archiveGoalsUseCase, fakeTimeProvider)

        /** SUT */
        editGoalUseCase = EditGoalUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)

        runBlocking {
            addGoal(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = 1.hours,
                ),
                libraryItemIds = null,
            )
        }
    }

    @Test
    fun `Edit goal target, target is changed`() = runTest {

        editGoalUseCase(
            descriptionId = UUIDConverter.fromInt(1),
            instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                target = 2.hours,
            ),
        )

        val goals = fakeGoalRepository.allGoals.first()

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
                        targetSeconds = 7200,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Edit goal target in the future, changes only latest instance`() = runTest {
        fakeTimeProvider.advanceTimeBy(1.days)

        updateGoalsUseCase()

        editGoalUseCase(
            descriptionId = UUIDConverter.fromInt(1),
            instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                target = 2.hours,
            ),
        )

        val goals = fakeGoalRepository.allGoals.first()

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
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
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
                        createdAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(1.days.toJavaDuration()),
                        targetSeconds = 7200,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Edit goal target with future instances, future instances are cleaned`() = runTest {
        fakeTimeProvider.advanceTimeBy(1.days)

        updateGoalsUseCase()

        fakeTimeProvider.revertTimeBy(1.days)

        editGoalUseCase(
            descriptionId = UUIDConverter.fromInt(1),
            instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                target = 2.hours,
            ),
        )

        val goals = fakeGoalRepository.allGoals.first()

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
                        targetSeconds = 7200,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Edit non-existent goal, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(2),
                instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                    target = 2.hours,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Goal with id 00000000-0000-0000-0000-000000000002 does not exist")
    }


    @Test
    fun `Edit goal with archived property set, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                descriptionUpdateAttributes = GoalDescriptionUpdateAttributes(
                    archived = true,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Use the archive goal use case to archive goals")
    }

    @Test
    fun `Edit goal with paused property set, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                descriptionUpdateAttributes = GoalDescriptionUpdateAttributes(
                    paused = true,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Use the pause goals use case to pause goals")
    }

    @Test
    fun `Edit goal with customOrder property set, NotImplementedError`() = runTest {
        val exception = assertThrows<NotImplementedError> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                descriptionUpdateAttributes = GoalDescriptionUpdateAttributes(
                    customOrder = Nullable(1),
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Custom order is not implemented yet")
    }

    @Test
    fun `Edit goal with endTimestamp property set, InvalidGoalDescriptionException`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                    endTimestamp = Nullable(FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration())),
                ),
            )
        }

        assertThat(exception.message).isEqualTo("End timestamp cannot be set manually")
    }

    @Test
    fun `Edit goal with target 0, InvalidGoalInstanceException`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                    target = 0.hours,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }

    @Test
    fun `Edit goal with infinite target, InvalidGoalInstanceException`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            editGoalUseCase(
                descriptionId = UUIDConverter.fromInt(1),
                instanceUpdateAttributes = GoalInstanceUpdateAttributes(
                    target = Double.POSITIVE_INFINITY.hours,
                ),
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }
}