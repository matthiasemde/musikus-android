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
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.usecase.AddGoalUseCase
import app.musikus.goals.domain.usecase.ArchiveGoalsUseCase
import app.musikus.goals.domain.usecase.CleanFutureGoalInstancesUseCase
import app.musikus.goals.domain.usecase.PauseGoalsUseCase
import app.musikus.goals.domain.usecase.UpdateGoalsUseCase
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class UpdateGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var addGoalUseCase: AddGoalUseCase
    private lateinit var pauseGoalsUseCase: PauseGoalsUseCase
    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase

    /** SUT */
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase

    private val goalDescriptionCreationAttributes = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.DAY,
    )

    private val goalInstanceCreationAttributes = GoalInstanceCreationAttributes(
        target = 1.hours,
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        addGoalUseCase = AddGoalUseCase(
            fakeGoalRepository,
            GetAllLibraryItemsUseCase(fakeLibraryRepository),
            fakeTimeProvider
        )
        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(
            fakeGoalRepository,
            fakeTimeProvider
        )
        archiveGoalsUseCase = ArchiveGoalsUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)
        pauseGoalsUseCase = PauseGoalsUseCase(
            fakeGoalRepository,
            cleanFutureGoalInstancesUseCase
        )

        /** SUT */
        updateGoalsUseCase = UpdateGoalsUseCase(
            goalRepository = fakeGoalRepository,
            archiveGoals = archiveGoalsUseCase,
            timeProvider = fakeTimeProvider,
        )
    }

    @Test
    fun `update basic goal`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(2.days)

        updateGoalsUseCase()

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
                        modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
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
                        createdAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(1.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(2.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(4),
                        createdAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(3),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(2.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    ),
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update non-repeat goal, goal is archived`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes.copy(
                repeat = false
            ),
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(2.days)

        updateGoalsUseCase()

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = false,
                    periodInPeriodUnits = 1,
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
                        modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(1.days.toJavaDuration())
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update paused goal, previous instance is removed`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(1.days)
        updateGoalsUseCase()

        pauseGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        fakeTimeProvider.advanceTimeBy(1.days)
        updateGoalsUseCase()


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
                        id = UUIDConverter.fromInt(4),
                        createdAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(2.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(2.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update goal in another time zone`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.moveToTimezone(ZoneId.of("Europe/Berlin"))

        fakeTimeProvider.advanceTimeBy(1.days)

        updateGoalsUseCase()

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
                        modifiedAt = FakeTimeProvider.START_TIME.plus(
                            1.days.toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(
                            (1.days - 1.hours).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin"))
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME.plus(
                            1.days.toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(
                            1.days.toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ).plus(
                            (1.days - 1.hours).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update paused first goalInstance`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        pauseGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        fakeTimeProvider.advanceTimeBy(5.days)

        updateGoalsUseCase()

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
                    paused = true,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(7),
                        createdAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(5.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
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
}