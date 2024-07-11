/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.InvalidGoalDescriptionException
import app.musikus.database.entities.InvalidGoalInstanceException
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.goals.domain.usecase.AddGoalUseCase
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
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.hours

class AddGoalUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    /** SUT */
    private lateinit var addGoal: AddGoalUseCase

    private val validDescriptionCreationAttributes = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.DAY,
    )

    private val validInstanceCreationAttributes = GoalInstanceCreationAttributes(
        target = 1.hours,
    )


    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        val getItems = GetAllLibraryItemsUseCase(fakeLibraryRepository)

        /** SUT */
        addGoal = AddGoalUseCase(fakeGoalRepository, getItems, fakeTimeProvider)
    }

    @Test
    fun `Add valid non-specific goal, goalRepository contains goal`() = runTest {
        addGoal(
            descriptionCreationAttributes = validDescriptionCreationAttributes,
            instanceCreationAttributes = validInstanceCreationAttributes,
            libraryItemIds = emptyList()
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
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Add valid item-specific goal, goalRepository contains goal`() = runTest {
        fakeLibraryRepository.addItem(
            LibraryItemCreationAttributes(
                name = "Item 1",
                colorIndex = 0,
                libraryFolderId = Nullable(null)
            )
        )

        addGoal(
            descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                type = GoalType.ITEM_SPECIFIC,
            ),
            instanceCreationAttributes = validInstanceCreationAttributes,
            libraryItemIds = listOf(UUIDConverter.fromInt(1))
        )

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(2),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.ITEM_SPECIFIC,
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
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(2),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = FakeTimeProvider.START_TIME
                        ),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = listOf(
                    LibraryItem(
                        id = UUIDConverter.fromInt(1),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        name = "Item 1",
                        colorIndex = 0,
                        customOrder = null,
                        libraryFolderId = null
                    )
                )
            )
        )
    }

    @Test
    fun `Add goal with period 0, InvalidGoalDescriptionException('Period in period units must be greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    periodInPeriodUnits = 0
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Period in period units must be greater than 0")
    }

    @Test
    fun `Add goal with target 0, InvalidGoalInstanceException('Target must be finite and greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    target = 0.hours
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }

    @Test
    fun `Add goal with infinite target, InvalidGoalInstanceException('Target must be finite and greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    target = Double.POSITIVE_INFINITY.hours
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }

    @Test
    fun `Add goal with startTimestamp set, InvalidGoalInstanceException`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    startTimestamp = fakeTimeProvider.now()
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Start timestamp must not be set, it is set automatically")
    }

    @Test
    fun `Add goal with goalDescriptionId set, InvalidGoalInstanceException('Goal description id must not be set, it is set automatically')`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    descriptionId = UUIDConverter.fromInt(1)
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Goal description id must not be set, it is set automatically")
    }

    @Test
    fun `Add non-specific goal with library items, InvalidGoalDescriptionException('Library items must be null or empty for non-specific goals')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = listOf(UUIDConverter.fromInt(1))
            )
        }

        assertThat(exception.message).isEqualTo("Library items must be null or empty for non-specific goals, but was [00000000-0000-0000-0000-000000000001]")
    }

    @Test
    fun `Add item-specific goal without library items, InvalidGoalDescriptionException('Item specific goals must have at least one library item')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    type = GoalType.ITEM_SPECIFIC
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Item specific goals must have at least one library item")
    }

    @Test
    fun `Add item-specific goal with non-existent library items, InvalidGoalDescriptionException('Library items do not exist')`() = runTest {
        fakeLibraryRepository.addItem(
            LibraryItemCreationAttributes(
                name = "Item 1",
                colorIndex = 0,
                libraryFolderId = Nullable(null)
            )
        )

        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    type = GoalType.ITEM_SPECIFIC
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = listOf(
                    UUIDConverter.fromInt(1),
                    UUIDConverter.fromInt(2)
                )
            )
        }

        assertThat(exception.message).isEqualTo("Library items do not exist: [00000000-0000-0000-0000-000000000002]")
    }
}