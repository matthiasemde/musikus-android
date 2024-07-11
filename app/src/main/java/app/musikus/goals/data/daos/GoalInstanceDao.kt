/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.goals.data.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.core.data.daos.TimestampDao
import app.musikus.core.data.GoalInstanceWithDescription
import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.MusikusDatabase
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceModel
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.core.data.entities.TimestampModelDisplayAttributes
import app.musikus.core.data.toDatabaseInterpretableString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class GoalInstance(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name="goal_description_id") val descriptionId: UUID,
    @ColumnInfo(name="previous_goal_instance_id") val previousInstanceId: UUID?,
    @ColumnInfo(name="start_timestamp") val startTimestamp: ZonedDateTime,
    @ColumnInfo(name="end_timestamp") val endTimestamp: ZonedDateTime?,
    @ColumnInfo(name="target_seconds") val targetSeconds: Long,
) : TimestampModelDisplayAttributes() {

    override fun toString(): String {
        return super.toString() +
            "\tgoalDescriptionId:\t$descriptionId\n" +
            "\tpreviousInstanceId:\t$previousInstanceId\n" +
            "\tstartTimestamp:\t\t$startTimestamp\n" +
            "\tendTimestamp:\t\t$endTimestamp\n" +
            "\ttarget:\t\t\t\t$target\n"
    }

    val target : Duration
        get() = targetSeconds.seconds
}

@Dao
abstract class GoalInstanceDao(
    private val database : MusikusDatabase,
) : TimestampDao<
        GoalInstanceModel,
        GoalInstanceCreationAttributes,
        GoalInstanceUpdateAttributes,
        GoalInstance
        >(
    tableName = "goal_instance",
    database = database,
    displayAttributes = listOf(
        "goal_description_id",
        "previous_goal_instance_id",
        "start_timestamp",
        "end_timestamp",
        "target_seconds",
    ),
    dependencies = listOf("goal_description")
) {

    /**
     * @Insert
     */

    override fun createModel(creationAttributes: GoalInstanceCreationAttributes): GoalInstanceModel {
        return GoalInstanceModel(
            descriptionId = creationAttributes.descriptionId,
            previousInstanceId = creationAttributes.previousInstanceId,
            startTimestamp = creationAttributes.startTimestamp,
            target = creationAttributes.target,
        )
    }

    override suspend fun insert(creationAttributes: GoalInstanceCreationAttributes): UUID {
        return insert(
            creationAttributes = creationAttributes,
            firstInstance = false
        )
    }

    override suspend fun insert(creationAttributes: List<GoalInstanceCreationAttributes>): List<UUID> {
        throw NotImplementedError("")
    }

    // create a new instance of this goal, storing the target for a single period
    suspend fun insert(
        creationAttributes: GoalInstanceCreationAttributes,
        firstInstance: Boolean
    ) : UUID {
        val descriptionId = creationAttributes.descriptionId

        // throws exception if description does not exist
        val description = database.goalDescriptionDao.getAsFlow(descriptionId).first()

        if(description.archived) {
            throw IllegalArgumentException("Cannot insert instance for archived goal: $descriptionId")
        }

        if(!firstInstance) {
            val previousInstanceId = creationAttributes.previousInstanceId
                ?: throw IllegalArgumentException("Cannot insert instance without providing id of previous instance")

            val instancesForDescription = getForDescription(descriptionId)

            if(instancesForDescription.any { it.previousInstanceId == previousInstanceId }) {
                throw IllegalArgumentException("Cannot insert instance with previous instance id that matches previous instance id of another instance for the same description")
            }

            // throws exception if previous instance does not exist
            val previousInstance = try {
                instancesForDescription.single { it.id == previousInstanceId }
            } catch(e: NoSuchElementException) {
                throw IllegalArgumentException("Goal description does not contain instance with id: $previousInstanceId")
            }

            if (previousInstance.endTimestamp == null) {
                throw IllegalArgumentException("Cannot insert instance before finalizing previous instance (endTimestamp is null)")
            }

            if (previousInstance.endTimestamp > creationAttributes.startTimestamp) {
                throw IllegalArgumentException("Cannot insert instance with startTimestamp before latest endTimestamp")
            }
        }

        return super.insert(listOf(creationAttributes)).single() // returns the id of the inserted instance
    }

    /**
     * @Update
     */

    // no need to override update(id, updateAttributes) since it calls update(rows) anyways
    override suspend fun update(rows: List<Pair<UUID, GoalInstanceUpdateAttributes>>) {
        // get all instances to be updated
        val instances = getAsFlow(rows.map { it.first }).first()

        // get all descriptions of the instances to be updated
        // throws exception if any description does not exist
        val descriptions = database.goalDescriptionDao.getAsFlow(
            instances.map { it.descriptionId }
        ).first()

        if(
            descriptions.any { it.archived } &&
            rows.any { (_, updateAttributes) -> updateAttributes.target != null }
        ) {
            throw IllegalArgumentException("Cannot update target for instance(s) of archived goal(s): ${descriptions.filter {it.archived}.map { it.id }}")
        }

        super.update(rows)
    }

    override fun applyUpdateAttributes(
        oldModel: GoalInstanceModel,
        updateAttributes: GoalInstanceUpdateAttributes
    ): GoalInstanceModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        endTimestamp = updateAttributes.endTimestamp ?: oldModel.endTimestamp
        target = updateAttributes.target ?: oldModel.target
    }

    /**
     * @Delete
     */

    override suspend fun delete(id: UUID) {
        throw NotImplementedError(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    override suspend fun delete(ids: List<UUID>) {
        throw NotImplementedError(
            "Instances are deleted automatically when their description is deleted or a paused goal is renewed"
        )
    }

    suspend fun deletePausedInstance(id: UUID) {
        // try to get the description of the instance
        // if null is returned, it must be because the instance does not exist
        val description = database.goalDescriptionDao.getDescriptionForInstance(id) ?:
            throw IllegalArgumentException("Could not find goal_instance with the following id: $id")

        if (!description.paused) {
            throw IllegalArgumentException("Can only delete instances of paused goals")
        }

        super.delete(listOf(id)) // need to call listOf(id) because super.delete(id) would call overridden delete(listOf(id))
    }

    suspend fun deleteFutureInstances(ids: List<UUID>) {
        val uniqueIds = ids.distinct()

        val instances = get(uniqueIds)

        if (instances.any { it.endTimestamp != null || it.startTimestamp < database.timeProvider.now() }) {
            throw IllegalArgumentException("Can only delete instances in the future")
        }

        super.delete(uniqueIds)
    }

    /**
     * @Queries
     */

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance WHERE " +
        "previous_goal_instance_id=:previousInstanceId"
    )
    abstract fun getByPreviousInstanceId(previousInstanceId: UUID?): Flow<GoalInstance>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE goal_description_id = (SELECT " +
            "id FROM goal_description " +
            "WHERE id = :descriptionId " +
            "AND deleted = 0" +
        ")"
    )
    protected abstract suspend fun directGetForDescription(
        descriptionId: UUID
    ): List<GoalInstance>

    suspend fun getForDescription(
        descriptionId: UUID
    ) : List<GoalInstance> {

        if(!database.goalDescriptionDao.exists(descriptionId)) {
            throw IllegalArgumentException("Cannot get instances for non-existing description: $descriptionId")
        }

        return directGetForDescription(descriptionId)
    }

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1)) <= datetime(:now) " +
            "AND (" +
                "end_timestamp IS NULL " +
                "OR datetime(:now) < datetime(SUBSTR(end_timestamp, 1, INSTR(end_timestamp, '[') - 1))" +
            ") " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE archived=0 " +
                "AND deleted=0" +
            ")"
    )
    protected abstract fun directGetCurrent(
        now: String = database.timeProvider.now().toDatabaseInterpretableString()
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    fun getCurrent() = directGetCurrent()


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "Select * FROM goal_instance " +
        "WHERE end_timestamp IS NULL " +
        "AND goal_description_id IN (" +
            "SELECT id FROM goal_description " +
            "WHERE deleted=0" +
        ") "
    )
    abstract suspend fun getLatest(): List<GoalInstanceWithDescription>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE deleted=0" +
                ")" +
                "AND end_timestamp IS NOT NULL " +
                "ORDER BY datetime(SUBSTR(end_timestamp, 1, INSTR(end_timestamp, '[') - 1)) DESC " +
                "LIMIT :n"
    )
    abstract fun getLastNCompleted(
        n: Int,
    ): Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
}