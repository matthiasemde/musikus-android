/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
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
import kotlin.time.toJavaDuration


@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class GoalInstanceDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var goalInstanceDao: GoalInstanceDao

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        goalInstanceDao = database.goalInstanceDao

        // Simulate a goal description which is renewed once already
        runBlocking {
            database.goalDescriptionDao.insert(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    startTimestamp = fakeTimeProvider.now(),
                    target = 30.minutes
                )
            )

            fakeTimeProvider.advanceTimeBy(1.days)

            goalInstanceDao.transaction {
                goalInstanceDao.update(
                    UUIDConverter.fromInt(2),
                    GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(fakeTimeProvider.now()),
                    )
                )

                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = UUIDConverter.fromInt(2),
                    startTimestamp = fakeTimeProvider.now(),
                    target = 35.minutes
                ))
            }
        }
    }

    @Test
    fun getAll() = runTest {

        // Get all instances
        val instances = goalInstanceDao.getAllAsFlow().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(2),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 1800,
                startTimestamp = fakeTimeProvider.startTime,
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = UUIDConverter.fromInt(2),
                targetSeconds = 2100,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun getAllWithDeletedGoal_noInstances() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        // Get all instances
        val instances = goalInstanceDao.getAllAsFlow().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).isEmpty()
    }

    @Test
    fun insertInstances_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                goalInstanceDao.insert(listOf(
                    GoalInstanceCreationAttributes(
                        descriptionId = UUIDConverter.fromInt(1),
                        target = 30.minutes,
                        startTimestamp = fakeTimeProvider.startTime
                    )
                ))
            }
        }

        assertThat(exception.message).isEqualTo("")
    }

    @Test
    fun insertInstance() = runTest {
        // Update the latest instance so we can insert a new one
        goalInstanceDao.update(
            UUIDConverter.fromInt(3),
            GoalInstanceUpdateAttributes(
                endTimestamp = Nullable(fakeTimeProvider.now()),
            )
        )

        // Insert a new instance
        val instanceId = goalInstanceDao.insert(GoalInstanceCreationAttributes(
            descriptionId = UUIDConverter.fromInt(1),
            previousInstanceId = UUIDConverter.fromInt(3),
            startTimestamp = fakeTimeProvider.now(),
            target = 40.minutes
        ))

        // Check if the correct id was returned
        assertThat(instanceId).isEqualTo(UUIDConverter.fromInt(4))

        // Check if the instance was inserted correctly
        val instances = goalInstanceDao.getAllAsFlow().first()

        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(2),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 1800,
                startTimestamp = fakeTimeProvider.startTime,
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = UUIDConverter.fromInt(2),
                targetSeconds = 2100,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(4),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = UUIDConverter.fromInt(3),
                targetSeconds = 2400,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun insertInstanceWithoutUpdatingPreviousInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(1),
                    startTimestamp = fakeTimeProvider.now(),
                    target = 40.minutes
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot insert instance before finalizing previous instance"
        )
    }

    @Test
    fun insertInstanceBeforeLatestEndTimestamp_throwsException() = runTest {
        // Update the latest instance so we can insert a new one
        goalInstanceDao.update(
            UUIDConverter.fromInt(3),
            GoalInstanceUpdateAttributes(
                endTimestamp = Nullable(fakeTimeProvider.now()),
            )
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(1),
                    startTimestamp = fakeTimeProvider.now().minus(1.days.toJavaDuration()),
                    target = 40.minutes
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot insert instance with startTimestamp before latest endTimestamp"
        )
    }

    @Test
    fun insertInstanceForNonExistentGoal_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(0),
                    startTimestamp = fakeTimeProvider.now(),
                    target = 40.minutes
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun insertInstanceForDeletedGoal_throwsException() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(1),
                    startTimestamp = fakeTimeProvider.now(),
                    target = 40.minutes
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_description(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun insertInstanceForArchivedGoal_throwsException() = runTest {
        // Archive the goal
        database.goalDescriptionDao.update(
            UUIDConverter.fromInt(1),
            GoalDescriptionUpdateAttributes(archived = true)
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.insert(GoalInstanceCreationAttributes(
                    descriptionId = UUIDConverter.fromInt(1),
                    startTimestamp = fakeTimeProvider.now(),
                    target = 40.minutes
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot insert instance for archived goal: 00000000-0000-0000-0000-000000000001"
        )
    }

    @Test
    fun updateInstances() = runTest {
        // Update the instances
        goalInstanceDao.update(listOf(
            UUIDConverter.fromInt(2) to GoalInstanceUpdateAttributes(target = 40.minutes),
            UUIDConverter.fromInt(3) to GoalInstanceUpdateAttributes(target = 45.minutes)
        ))

        // Check if the instances were updated correctly
        val updatedInstances = goalInstanceDao.getAllAsFlow().first()

        assertThat(updatedInstances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(2),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 2400,
                startTimestamp = fakeTimeProvider.startTime,
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = UUIDConverter.fromInt(2),
                targetSeconds = 2700,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun updateInstance() = runTest {
        val updateAttributes = GoalInstanceUpdateAttributes(
            endTimestamp = Nullable(fakeTimeProvider.now()),
            target = 10.minutes
        )

        val goalInstanceDaoSpy = spyk(goalInstanceDao)

        try {
            goalInstanceDaoSpy.update(
                UUIDConverter.fromInt(1),
                updateAttributes
            )
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) {
            goalInstanceDaoSpy.update(listOf(
                UUIDConverter.fromInt(1) to updateAttributes
            ))
        }
    }

    @Test
    fun updateNonExistentInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.update(
                    UUIDConverter.fromInt(0),
                    GoalInstanceUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun updateInstanceOfDeletedGoal_throwsException() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.update(
                    UUIDConverter.fromInt(2),
                    GoalInstanceUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun updateTargetForInstanceOfArchivedGoal_throwsException() = runTest {
        // Archive the goal
        database.goalDescriptionDao.update(
            UUIDConverter.fromInt(1),
            GoalDescriptionUpdateAttributes(archived = true)
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.update(
                    UUIDConverter.fromInt(2),
                    GoalInstanceUpdateAttributes(target = 40.minutes)
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot update target for instance(s) of archived goal(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun deleteInstances_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                goalInstanceDao.delete(listOf(
                    UUIDConverter.fromInt(1),
                    UUIDConverter.fromInt(2)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    @Test
    fun deleteInstance_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                goalInstanceDao.delete(UUIDConverter.fromInt(1))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    @Test
    fun deletePausedGoalInstance() = runTest {
        // pause the goal so instances can be deleted
        database.goalDescriptionDao.update(
            UUIDConverter.fromInt(1),
            GoalDescriptionUpdateAttributes(paused = true)
        )

        // delete the instance
        goalInstanceDao.deletePausedInstance(UUIDConverter.fromInt(2))

        // check if the instance is no longer in the list of instances
        val instances = goalInstanceDao.getAllAsFlow().first()

        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 2100,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun deletePausedGoalInstanceOnNonExistentInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deletePausedInstance(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance with the following id: 00000000-0000-0000-0000-000000000000"
        )
    }

    @Test
    fun deletePausedGoalInstanceOnNonPausedGoalInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deletePausedInstance(UUIDConverter.fromInt(3))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Can only delete instances of paused goals"
        )
    }

    @Test
    fun deleteFutureGoalInstances() = runTest {
        // Revert the time to make the latest instance be in the future
        fakeTimeProvider.revertTimeBy(1.days)

        goalInstanceDao.deleteFutureInstances(listOf(
            UUIDConverter.fromInt(3)
        ))

        // check if the instance was correctly
        val instances = goalInstanceDao.getAllAsFlow().first()

        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(2),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 1800,
                startTimestamp = fakeTimeProvider.startTime,
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            )
        )
    }

    @Test
    fun deleteFutureGoalInstancesOnNonExistentInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deleteFutureInstances(listOf(
                    UUIDConverter.fromInt(0)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun deleteFutureGoalInstancesOfDeletedGoals_throwsException() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deleteFutureInstances(listOf(
                    UUIDConverter.fromInt(2)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun deleteFutureGoalInstancesOnGoalInPast_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deleteFutureInstances(listOf(
                    UUIDConverter.fromInt(2)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Can only delete instances in the future"
        )
    }

    @Test
    fun deleteFutureGoalInstancesOnInstanceWithEndTimestamp_throwsException() = runTest {
        fakeTimeProvider.revertTimeBy(1.days)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.deleteFutureInstances(listOf(
                    UUIDConverter.fromInt(2)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Can only delete instances in the future"
        )
    }

    @Test
    fun getSpecificInstances() = runTest {
        // Insert a few more instances
        repeat(2) { index ->
            fakeTimeProvider.advanceTimeBy(1.days)

            goalInstanceDao.update(
                UUIDConverter.fromInt(3 + index),
                GoalInstanceUpdateAttributes(
                    endTimestamp = Nullable(fakeTimeProvider.now()),
                )
            )

            goalInstanceDao.insert(GoalInstanceCreationAttributes(
                descriptionId = UUIDConverter.fromInt(1),
                startTimestamp = fakeTimeProvider.now(),
                target = (40 + 5*index).minutes
            ))
        }

        // Get the instances
        val instances = goalInstanceDao.getAsFlow(listOf(
            UUIDConverter.fromInt(3),
            UUIDConverter.fromInt(5)
        )).first()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 2100,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(5),
                createdAt = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 2700,
                startTimestamp = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun getSpecificInstance() = runTest {
        val goalInstanceDaoSpy = spyk(goalInstanceDao)

        goalInstanceDaoSpy.getAsFlow(UUIDConverter.fromInt(2))

        coVerify (exactly = 1) {
            goalInstanceDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getSpecificInstancesOfDeletedGoal_throwsException() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.getAsFlow(listOf(
                    UUIDConverter.fromInt(2),
                    UUIDConverter.fromInt(3)
                )).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): " +
            "[" +
            "00000000-0000-0000-0000-000000000002, " +
            "00000000-0000-0000-0000-000000000003" +
            "]"
        )
    }

    @Test
    fun getNonExistentInstance_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.getAsFlow(UUIDConverter.fromInt(1)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal_instance(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun instanceExists() = runTest {
        assertThat(goalInstanceDao.exists(UUIDConverter.fromInt(2))).isTrue()
    }

    @Test
    fun instanceOfDeletedGoalDoesNotExists() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        assertThat(goalInstanceDao.exists(UUIDConverter.fromInt(2))).isFalse()
    }

    @Test
    fun instanceDoesNotExist() = runTest {
        assertThat(goalInstanceDao.exists(UUIDConverter.fromInt(1))).isFalse()
    }

    @Test
    fun getForDescription() = runTest {
        // Insert another goal
        database.goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                startTimestamp = fakeTimeProvider.now(),
                target = 5.minutes
            )
        )

        // Get the instances of the first goal
        val instances = goalInstanceDao.getForDescription(UUIDConverter.fromInt(1))

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstance(
                id = UUIDConverter.fromInt(2),
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 1800,
                startTimestamp = fakeTimeProvider.startTime,
                endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
            ),
            GoalInstance(
                id = UUIDConverter.fromInt(3),
                createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                descriptionId = UUIDConverter.fromInt(1),
                previousInstanceId = null,
                targetSeconds = 2100,
                startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                endTimestamp = null,
            )
        )
    }

    @Test
    fun getForDescriptionOfNonExistentGoal_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.getForDescription(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot get instances for non-existing description: 00000000-0000-0000-0000-000000000000"
        )
    }

    @Test
    fun getForDescriptionOfDeletedGoal_throwsException() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                goalInstanceDao.getForDescription(UUIDConverter.fromInt(1))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Cannot get instances for non-existing description: 00000000-0000-0000-0000-000000000001"
        )
    }

    @Test
    fun getCurrent() = runTest {
        // Insert another goal
        database.goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                startTimestamp = fakeTimeProvider.now(),
                target = 5.minutes
            )
        )

        // Get the instances
        val instances = goalInstanceDao.getCurrent().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(3),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = null,
                    targetSeconds = 2100,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = null,
                ),
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(1),
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = false,
                        archived = false,
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                    ),
                    libraryItems = emptyList()
                )
            ),
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(5),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(4),
                    previousInstanceId = null,
                    targetSeconds = 300,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = null,
                ),
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(4),
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 2,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = false,
                        archived = false,
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    ),
                    libraryItems = emptyList()
                )
            )
        )
    }

    @Test
    fun getCurrentInThePast() = runTest {
        fakeTimeProvider.revertTimeBy(1.days)

        // Get the instances
        val instances = goalInstanceDao.getCurrent().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(2),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = null,
                    targetSeconds = 1800,
                    startTimestamp = fakeTimeProvider.startTime,
                    endTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                ),
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(1),
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = false,
                        archived = false,
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                    ),
                    libraryItems = emptyList()
                )
            )
        )
    }

    @Test
    fun getCurrentForArchivedGoal_noInstances() = runTest {
        // Archive the goal
        database.goalDescriptionDao.update(
            UUIDConverter.fromInt(1),
            GoalDescriptionUpdateAttributes(archived = true)
        )

        // Get the instances
        val instances = goalInstanceDao.getCurrent().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).isEmpty()
    }

    @Test
    fun getCurrentForDeletedGoal_noInstances() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        // Get the instances
        val instances = goalInstanceDao.getCurrent().first()

        // Check if the instances were retrieved correctly
        assertThat(instances).isEmpty()
    }

    @Test
    fun getLatest() = runTest {
        // Insert another goal
        database.goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                startTimestamp = fakeTimeProvider.now(),
                target = 5.minutes
            )
        )

        // Get the instances
        val instances = goalInstanceDao.getLatest()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstanceWithDescription(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(3),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = null,
                    targetSeconds = 2100,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = null,
                ),
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null,
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                )
            ),
            GoalInstanceWithDescription(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(5),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(4),
                    previousInstanceId = null,
                    targetSeconds = 300,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = null,
                ),
                description = GoalDescription(
                    id = UUIDConverter.fromInt(4),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null,
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                )
            )
        )
    }

    @Test
    fun getLastNCompleted() = runTest {
        // Insert another goal
        database.goalDescriptionDao.insert(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 2,
                periodUnit = GoalPeriodUnit.DAY
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                startTimestamp = fakeTimeProvider.now(),
                target = 1.hours
            )
        )

        fakeTimeProvider.advanceTimeBy(1.days)

        goalInstanceDao.update(
            UUIDConverter.fromInt(3),
            GoalInstanceUpdateAttributes(
                endTimestamp = Nullable(fakeTimeProvider.now()),
            )
        )

        fakeTimeProvider.advanceTimeBy(1.days)

        goalInstanceDao.update(
            UUIDConverter.fromInt(5),
            GoalInstanceUpdateAttributes(
                endTimestamp = Nullable(fakeTimeProvider.now()),
            )
        )


        // Get the instances
        val instances = goalInstanceDao.getLastNCompleted(n = 2).first()

        // Check if the instances were retrieved correctly
        assertThat(instances).containsExactly(
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(3),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(1),
                    previousInstanceId = null,
                    targetSeconds = 2100,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                ),
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(1),
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = false,
                        archived = false,
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                    ),
                    libraryItems = emptyList()
                )
            ),
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(5),
                    createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                    descriptionId = UUIDConverter.fromInt(4),
                    previousInstanceId = null,
                    targetSeconds = 3600,
                    startTimestamp = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    endTimestamp = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                ),
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(4),
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 2,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = false,
                        archived = false,
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                    ),
                    libraryItems = emptyList()
                )
            )
        )
    }

    @Test
    fun getLastNCompletedForDeletedGoal_noInstances() = runTest {
        // Delete the goal
        database.goalDescriptionDao.delete(UUIDConverter.fromInt(1))

        // Get the instances
        val instances = goalInstanceDao.getLastNCompleted(n = 2).first()

        // Check if the instances were retrieved correctly
        assertThat(instances).isEmpty()
    }
}