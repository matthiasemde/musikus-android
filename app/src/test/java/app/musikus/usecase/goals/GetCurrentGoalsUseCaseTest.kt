/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
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
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class GetCurrentGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    private lateinit var sortGoalsUseCase: SortGoalsUseCase

    /** SUT */
    private lateinit var getCurrentGoals: GetCurrentGoalsUseCase

    private val baseDescription = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.DAY,
        progressType = GoalProgressType.TIME,
    )

    private val baseInstance = GoalInstanceCreationAttributes(
        descriptionId = UUIDConverter.fromInt(1),
        previousInstanceId = null,
        startTimestamp = FakeTimeProvider.START_TIME,
        target = 1.hours
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()

        sortGoalsUseCase = SortGoalsUseCase(fakeUserPreferencesRepository)

        /** SUT */
        getCurrentGoals = GetCurrentGoalsUseCase(
            goalRepository = fakeGoalRepository,
            sortGoals = sortGoalsUseCase,
        )

        runBlocking {
            fakeGoalRepository.addNewGoal(
                descriptionCreationAttributes = baseDescription,
                instanceCreationAttributes = baseInstance,
                libraryItemIds = null
            )

            fakeGoalRepository.updateGoalDescriptions(listOf(
                UUIDConverter.fromInt(1) to
                GoalDescriptionUpdateAttributes(paused = true)
            ))
        }
    }

    @Test
    fun `Get current goals including paused, returns goal`() = runTest {
        val goals = getCurrentGoals(excludePaused = false).first()

        assertThat(goals).containsExactly(
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(2),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = null,
                    startTimestamp = FakeTimeProvider.START_TIME,
                    targetSeconds = 3600,
                    endTimestamp = null
                ),
                description = GoalDescriptionWithLibraryItems(
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
                    libraryItems = emptyList()
                ),
            )
        )
    }

    @Test
    fun `Get current goals excluding paused, returns nothing`() = runTest {
        val goals = getCurrentGoals(excludePaused = true).first()

        assertThat(goals).isEmpty()
    }
}