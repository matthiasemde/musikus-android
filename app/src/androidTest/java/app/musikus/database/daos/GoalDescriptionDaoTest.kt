/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.di.AppModule
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class GoalDescriptionDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var goalDescriptionDao: GoalDescriptionDao

    @Inject
    lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        goalDescriptionDao = database.goalDescriptionDao
    }

    @Test
    fun insertGoalDescriptions_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                goalDescriptionDao.insert(listOf(
                    GoalDescriptionCreationAttributes(
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY
                    )
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Use overload insert(description, instanceCreationAttributes, libraryItemIds) instead"
        )
    }

    @Test
    fun insertGoalDescription_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                goalDescriptionDao.insert(GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Use overload insert(description, instanceCreationAttributes, libraryItemIds) instead"
        )
    }

    @Test
    fun insertNonSpecificGoal() = runTest {
        val (descriptionId, instanceId) = goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 10.minutes,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        // check if the correct ids were returned
        assertThat(descriptionId).isEqualTo(UUIDConverter.fromInt(1))
        assertThat(instanceId).isEqualTo(UUIDConverter.fromInt(2))

        // check if the correct data was inserted
        val descriptions = goalDescriptionDao.getAllAsFlow().first()

        assertThat(descriptions).containsExactly(
            GoalDescription(
                id = UUIDConverter.fromInt(1),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            )
        )
    }

    @Test
    fun insertItemSpecificGoal() = runTest {
        database.libraryItemDao.insert(
            LibraryItemCreationAttributes(
                name = "TestItem",
                colorIndex = 1,
                libraryFolderId = Nullable(null),
            )
        )

        val (descriptionId, instanceId) = goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.WEEK
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 2.hours,
                startTimestamp = fakeTimeProvider.now(),
            ),
            libraryItemIds = listOf(
                UUIDConverter.fromInt(1)
            )
        )

        // check if the correct ids were returned
        assertThat(descriptionId).isEqualTo(UUIDConverter.fromInt(2))
        assertThat(instanceId).isEqualTo(UUIDConverter.fromInt(3))

        val goalDescriptionWithLibraryItems = goalDescriptionDao
            .getAllWithInstancesAndLibraryItems()
            .first()

        assertThat(goalDescriptionWithLibraryItems).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(2),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.ITEM_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.WEEK,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null,
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        descriptionId = UUIDConverter.fromInt(2),
                        previousInstanceId = null,
                        targetSeconds = 7200,
                        startTimestamp = fakeTimeProvider.startTime,
                        endTimestamp = null,
                    )
                ),
                libraryItems = listOf(
                    LibraryItem(
                        id = UUIDConverter.fromInt(1),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        name = "TestItem",
                        colorIndex = 1,
                        libraryFolderId = null,
                        customOrder = null,
                    )
                )
            )
        )
    }

    @Test
    fun insertNonSpecificGoalWithLibraryItems_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.insert(
                    descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY
                    ),
                    instanceCreationAttributes = GoalInstanceCreationAttributes(
                        target = 10.minutes,
                        startTimestamp = fakeTimeProvider.now(),
                    ),
                    libraryItemIds = listOf(
                        UUIDConverter.fromInt(1)
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Non-specific goals cannot have library items"
        )
    }

    @Test
    fun insertItemSpecificGoalWithoutLibraryItems_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.insert(
                    descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                        type = GoalType.ITEM_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 2,
                        periodUnit = GoalPeriodUnit.WEEK
                    ),
                    instanceCreationAttributes = GoalInstanceCreationAttributes(
                        target = 2.hours,
                        startTimestamp = fakeTimeProvider.now(),
                    ),
                    libraryItemIds = null
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Specific goals must have at least one library item"
        )
    }

    @Test
    fun insertItemSpecificGoalWithEmptyListOfLibraryItems_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.insert(
                    descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                        type = GoalType.ITEM_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 2,
                        periodUnit = GoalPeriodUnit.WEEK
                    ),
                    instanceCreationAttributes = GoalInstanceCreationAttributes(
                        target = 2.hours,
                        startTimestamp = fakeTimeProvider.now(),
                    ),
                    libraryItemIds = emptyList()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Specific goals must have at least one library item"
        )
    }



    @Test
    fun updateGoalDescriptions() = runTest {
        // insert two goals
        repeat(2) { index ->
            goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = index + 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = (index + 1).hours,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        }

        fakeTimeProvider.advanceTimeBy(1.seconds)

        goalDescriptionDao.update(
            listOf(
                UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(
                    paused = true,
                ),
                UUIDConverter.fromInt(3) to GoalDescriptionUpdateAttributes(
                    paused = true,
                    archived = true,
                ),
            )
        )

        val goalDescriptions = goalDescriptionDao.getAllAsFlow().first()

        assertThat(goalDescriptions).containsExactly(
            GoalDescription(
                id = UUIDConverter.fromInt(1),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = true,
                archived = false,
                customOrder = null,
            ),
            GoalDescription(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = true,
                archived = true,
                customOrder = null,
            )
        )
    }


    @Test
    fun updateGoalDescription() = runTest {
        val goalDescriptionDaoSpy = spyk(goalDescriptionDao)

        try {
            goalDescriptionDaoSpy.update(UUIDConverter.fromInt(1), GoalDescriptionUpdateAttributes())
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            goalDescriptionDaoSpy.update(UUIDConverter.fromInt(1), GoalDescriptionUpdateAttributes())
        }
    }

    @Test
    fun updateNonExistentGoalDescription_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.update(
                    UUIDConverter.fromInt(0),
                    GoalDescriptionUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun deleteGoalDescriptions() = runTest {
        // insert two goals
        repeat(2) { index ->
            goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = index + 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = (index + 1).hours,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        }
        goalDescriptionDao.delete(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(3),
        ))

        val goalDescriptions = goalDescriptionDao.getAllAsFlow().first()

        assertThat(goalDescriptions).isEmpty()
    }

    @Test
    fun deleteGoalDescription() = runTest {
        val goalDescriptionDaoSpy = spyk(goalDescriptionDao)

        try {
            goalDescriptionDaoSpy.delete(UUIDConverter.fromInt(1))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) { goalDescriptionDaoSpy.delete(listOf(UUIDConverter.fromInt(1))) }
    }

    @Test
    fun deleteNonExistentDescription_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreDescriptions() = runTest {
        repeat(2) { index ->
            goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = index + 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = (index + 1).hours,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        }

        goalDescriptionDao.delete(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(3),
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        goalDescriptionDao.restore(listOf(
                UUIDConverter.fromInt(1),
                UUIDConverter.fromInt(3),
        ))

        val goalDescriptions = goalDescriptionDao.getAllAsFlow().first()

        assertThat(goalDescriptions).containsExactly(
            GoalDescription(
                id = UUIDConverter.fromInt(1),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            ),
            GoalDescription(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            )
        )
    }

    @Test
    fun restoreDescription() = runTest {
        val goalDescriptionDaoSpy = spyk(goalDescriptionDao)

        try {
            goalDescriptionDaoSpy.restore(UUIDConverter.fromInt(1))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            goalDescriptionDaoSpy.restore(listOf(UUIDConverter.fromInt(1)))
        }
    }

    @Test
    fun restoreNonExistentDescription_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.restore(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificGoalDescriptions() = runTest {
        repeat(3) { index ->
            goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = index + 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = (index + 1).hours,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        }

        val goalDescriptions = goalDescriptionDao.getAsFlow(listOf(
                UUIDConverter.fromInt(1),
                UUIDConverter.fromInt(5),
        )).first()

        assertThat(goalDescriptions).containsExactly(
            GoalDescription(
                id = UUIDConverter.fromInt(1),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            ),
            GoalDescription(
                id = UUIDConverter.fromInt(5),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 3,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            )
        )
    }

    @Test
    fun getSpecificDescription() = runTest {
        val goalDescriptionDaoSpy = spyk(goalDescriptionDao)

        try {
            goalDescriptionDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            goalDescriptionDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getNonExistentDescription_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalDescriptionDao.getAsFlow(UUIDConverter.fromInt(4)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000004]"
        )
    }

    @Test
    fun goalDescriptionExists() = runTest {
        goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 1.hours,
                startTimestamp = fakeTimeProvider.now(),
            )
        )
        assertThat(goalDescriptionDao.exists(UUIDConverter.fromInt(1))).isTrue()
    }

    @Test
    fun deletedGoalDescriptionDoesNotExist() = runTest {
        goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 1.hours,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        assertThat(goalDescriptionDao.exists(UUIDConverter.fromInt(1))).isFalse()
    }

    @Test
    fun goalDescriptionDoesNotExist() = runTest {
        assertThat(goalDescriptionDao.exists(UUIDConverter.fromInt(2))).isFalse()
    }

    @Test
    fun cleanGoalDescriptions() = runTest {
        repeat(2) { index ->
            goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = index + 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = (index + 1).hours,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        }

        goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        // advance time by a few days
        fakeTimeProvider.advanceTimeBy(4.days)

        goalDescriptionDao.delete(UUIDConverter.fromInt(3))

        // advance time by just under a month and clean items
        fakeTimeProvider.advanceTimeBy(28.days)

        goalDescriptionDao.clean()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Restoring goalDescription with id 1 should be impossible to restore
                // because it was cleaned
                // goalDescription with id 3 should be restored
                goalDescriptionDao.restore(
                    listOf(
                        UUIDConverter.fromInt(1),
                        UUIDConverter.fromInt(3)
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )

        // finally, check whether goalDescription 2's instance was removed as per foreign key constraint
        assertThat(database.goalInstanceDao.exists(UUIDConverter.fromInt(2))).isFalse()
    }

    @Test
    fun getDescriptionForInstance() = runTest {
        goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 1.hours,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        val goalDescription = goalDescriptionDao.getDescriptionForInstance(UUIDConverter.fromInt(2))

        assertThat(goalDescription).isEqualTo(
            GoalDescription(
                id = UUIDConverter.fromInt(1),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null,
            )
        )
    }

    @Test
    fun getAllWithInstancesAndLibraryItems() = runTest {
        // insert two library items
        repeat(2) {
            database.libraryItemDao.insert(
                LibraryItemCreationAttributes(
                    name = "TestItem$it",
                    colorIndex = it,
                    libraryFolderId = Nullable(null),
                )
            )
        }

        // insert a non-specific goal
        goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 1.hours,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        // insert an item-specific goal
        goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = false,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.WEEK
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                target = 2.hours,
                startTimestamp = fakeTimeProvider.now(),
            ),
            libraryItemIds = listOf(
                UUIDConverter.fromInt(1),
                UUIDConverter.fromInt(2),
            )
        )

        val descriptionsWithInstancesAndLibraryItems = goalDescriptionDao
            .getAllWithInstancesAndLibraryItems()
            .first()

        assertThat(descriptionsWithInstancesAndLibraryItems).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(3),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null,
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(4),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        descriptionId = UUIDConverter.fromInt(3),
                        previousInstanceId = null,
                        targetSeconds = 3600,
                        startTimestamp = fakeTimeProvider.startTime,
                        endTimestamp = null,
                    )
                ),
                libraryItems = emptyList()
            ),
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(5),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.ITEM_SPECIFIC,
                    repeat = false,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.WEEK,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null,
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(6),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        descriptionId = UUIDConverter.fromInt(5),
                        previousInstanceId = null,
                        targetSeconds = 7200,
                        startTimestamp = fakeTimeProvider.startTime,
                        endTimestamp = null,
                    )
                ),
                libraryItems = listOf(
                    LibraryItem(
                        id = UUIDConverter.fromInt(1),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        name = "TestItem0",
                        colorIndex = 0,
                        libraryFolderId = null,
                        customOrder = null,
                    ),
                    LibraryItem(
                        id = UUIDConverter.fromInt(2),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        name = "TestItem1",
                        colorIndex = 1,
                        libraryFolderId = null,
                        customOrder = null,
                    )
                )
            )
        )
    }
}